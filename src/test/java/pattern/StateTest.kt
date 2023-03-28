package pattern

import org.junit.jupiter.api.Test

enum class State {
    READY {
        override fun action(action: Action): State {
            return when (action) {
                Action.RUN -> RUNNING
                Action.STOP -> STOPPED
                else -> this
            }
        }
    },
    RUNNING {
        override fun action(action: Action): State {
            return when (action) {
                Action.PAUSE -> SUSPENDED
                Action.STOP -> STOPPED
                else -> this
            }
        }
    },
    SUSPENDED {
        override fun action(action: Action): State {
            return when (action) {
                Action.RESUME -> RUNNING
                Action.STOP -> STOPPED
                else -> this
            }
        }
    },
    SUCCESS, FAILURE, STOPPED;

    open fun action(action: Action): State {
        return this
    }

    open fun onEnter() {
        println("$this 에 진입 하였습니다.")
    }

    open fun onLeave() {
        println("$this 에서 나감")
    }

    fun isFinished(): Boolean {
        return listOf(STOPPED, SUCCESS, FAILURE).contains(this)
    }
}

enum class Action {
    RUN, PAUSE, RESUME, STOP
}

class JobStateManager {
    var currentState = State.READY

    fun action(action: Action) {
        println()

        val nextState = currentState.action(action)
        if (currentState == nextState) {
            println("상태 변화 없음")
        } else {
            currentState.onLeave()

            println("액션 $action 에 의해 상태 $currentState 에서 $nextState 변경")
            currentState = nextState
            currentState.onEnter()

            if (currentState.isFinished()) {
                println("프로세스가 종료 되었습니다.")
            }
        }
    }

    fun isFinished(): Boolean = currentState.isFinished()
}

class StateTest {

    @Test
    fun test() {
        val manager = JobStateManager()
        manager.action(Action.RUN)
        manager.action(Action.PAUSE)
        manager.action(Action.RESUME)
        manager.action(Action.STOP)
        manager.action(Action.STOP)
    }
}
