package ru.scoltech.openran.speedtest.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller
import java.util.concurrent.atomic.AtomicReference

class TaskChainBuilder<S : Any?> {
    private var tasks = mutableListOf<Task<out Any?, out Any?>>()
    private var onStop: () -> Unit = {}
    private var onFatalError: (String, Exception?) -> Unit = { _, _ -> }

    fun initializeNewChain(): TaskConsumer<S> {
        tasks = mutableListOf()
        return TasksCollector(tasks)
    }

    @Suppress("UNCHECKED_CAST")
    fun finishChainCreation(): TaskChain<S> {
        return TaskChain(tasks as MutableList<Task<Any?, Any?>>, onStop, onFatalError)
    }

    fun onStop(onStop: () -> Unit): TaskChainBuilder<S> {
        this.onStop = onStop
        return this
    }

    fun onFatalError(onFatalError: (String, Exception?) -> Unit): TaskChainBuilder<S> {
        this.onFatalError = onFatalError
        return this
    }

    private class TasksCollector<T : Any?>(
        private val tasks: MutableList<Task<*, *>>
    ) : TaskConsumer<T> {
        override fun <R : Any?> andThen(task: Task<T, R>): TaskConsumer<R> {
            tasks.add(task)
            return TasksCollector(tasks)
        }

        override fun <R : Any?> andThenUnstoppable(task: (T) -> R): TaskConsumer<R> {
            return andThen(UnstoppableTask(task))
        }

        override fun <A : Any?, R : Any?> andThenTry(
            startTask: Task<T, A>,
            buildBlock: TaskConsumer<A>.() -> TaskConsumer<R>,
        ): TaskConsumer.FinallyTaskConsumer<A, R> {
            return object : TaskConsumer.FinallyTaskConsumer<A, R> {
                override fun andThenFinally(task: (A) -> Unit): TaskConsumer<R> {
                    tasks.add(TryTask(startTask, buildBlock, task))
                    return TasksCollector(tasks)
                }
            }
        }
    }

    private class UnstoppableTask<T : Any?, R : Any?>(private val task: (T) -> R) : Task<T, R> {
        override fun prepare(
            argument: T,
            killer: TaskKiller,
        ): Promise<(R) -> Unit, (String, Exception?) -> Unit> {
            return Promise { onSuccess, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    onSuccess?.invoke(task(argument))
                }
            }
        }
    }

    private class TryTask<T : Any?, A : Any?, R : Any?>(
        private val startTask: Task<T, A>,
        buildBlock: TaskConsumer<A>.() -> TaskConsumer<R>,
        private val finallyTask: (A) -> Unit,
    ) : Task<T, R> {
        private val tasks: List<Task<out Any?, out Any?>>

        init {
            val mutableTasks = mutableListOf<Task<*, *>>()
            buildBlock(TasksCollector(mutableTasks))
            tasks = mutableTasks
        }

        private fun ((String, Exception?) -> Unit)?.withEndTask(
            finallyArgument: AtomicReference<A>
        ): ((String, Exception?) -> Unit) {
            return { message, exception ->
                finallyTask(finallyArgument.get())
                this?.invoke(message, exception)
            }
        }

        override fun prepare(
            argument: T,
            killer: TaskKiller,
        ): Promise<(R) -> Unit, (String, Exception?) -> Unit> = Promise { onSuccess, onError ->
            val finallyArgument = AtomicReference<A>()
            val onSuccessTask = UnstoppableTask<R, Unit> {
                finallyTask(finallyArgument.get())
                onSuccess?.invoke(it)
            }

            @Suppress("UNCHECKED_CAST")
            val tryChain = TaskChain<A>(
                (tasks + listOf(onSuccessTask)) as List<Task<Any?, Any?>>,
                { onError.withEndTask(finallyArgument).invoke("Stopped", null) },
                onError.withEndTask(finallyArgument)
            )
            val startTaskKiller = TaskKiller()

            killer.register {
                startTaskKiller.kill()
                tryChain.stop()
            }

            try {
                startTask.prepare(argument, startTaskKiller)
                    .onSuccess {
                        finallyArgument.set(it)
                        tryChain.start(it)
                    }
                    .onError(onError ?: { _, _ -> })
                    .start()
            } catch (e: FatalException) {
                onError?.invoke(e.message!!, e.cause as? Exception)
            }
        }
    }
}
