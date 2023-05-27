package com.diplomski.mucnjak.coco.ui.split_screen.solving

import androidx.lifecycle.viewModelScope
import com.diplomski.mucnjak.coco.data.ui.Answer
import com.diplomski.mucnjak.coco.data.ui.Question
import com.diplomski.mucnjak.coco.domain.repositories.state_machine.State
import com.diplomski.mucnjak.coco.domain.use_case.get_all_answers.GetAllAnswers
import com.diplomski.mucnjak.coco.domain.use_case.get_available_question.GetAvailableQuestion
import com.diplomski.mucnjak.coco.domain.use_case.get_student_name.GetStudentName
import com.diplomski.mucnjak.coco.domain.use_case.rotate_student_screen.RotateStudentScreen
import com.diplomski.mucnjak.coco.domain.use_case.subscribe_to_navigation_state.SubscribeToNavigationState
import com.diplomski.mucnjak.coco.domain.use_case.subscribe_to_timer_ticks.SubscribeToTimerTicks
import com.diplomski.mucnjak.coco.shared.DoNothing
import com.diplomski.mucnjak.coco.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AfterSolvingNavigationEvent {
    object NavigateToIncorrectSolution : AfterSolvingNavigationEvent()
}

@HiltViewModel
class SolvingViewModel @Inject constructor(
    private val getStudentName: GetStudentName,
    private val getAvailableQuestion: GetAvailableQuestion,
    private val getAllAnswers: GetAllAnswers,
    private val rotateStudentScreen: RotateStudentScreen,
    private val subscribeToTimerTicks: SubscribeToTimerTicks,
    private val subscribeToNavigationState: SubscribeToNavigationState,
) : BaseViewModel<SolvingState, AfterSolvingNavigationEvent>(SolvingState.Initial) {

    private var question: Question? = null
    private val answers: MutableList<Answer> = mutableListOf()
    private val selectedAnswers: MutableList<Answer> = mutableListOf()
    private var time: Int = 0

    fun init(index: Int) {
        viewModelScope.launch {
            val studentName = getStudentName(index)
            question = getAvailableQuestion(index)
            answers.addAll(getAllAnswers())

            updateState {
                SolvingState.Solving(
                    studentName = studentName,
                    question = question?.questionText ?: throw IllegalStateException(),
                    answers = answers,
                    selectedAnswers = selectedAnswers,
                    time = "10:00",
                )
            }

            launch {
                subscribeToTimerTicks().collect { time ->
                    this@SolvingViewModel.time = time
                    setState {
                        (it as? SolvingState.Solving)?.copy(time = "$time")
                            ?: (it as? SolvingState.Congratulations)?.copy(time = "$time") ?: it
                    }
                }
            }

            launch {
                subscribeToNavigationState().collect {
                    if (it == State.INCORRECT_SOLUTION_NOTE) {
                        setNavigationEvent(AfterSolvingNavigationEvent.NavigateToIncorrectSolution)
                    } else if (it == State.FINISH_NOTE) {
                        DoNothing
                    }
                }
            }
        }
    }

    fun rotateScreen(index: Int) {
        viewModelScope.launch {
            rotateStudentScreen(index)
        }
    }

    fun selectAnswer(answer: Answer) {
        if (answers.any { it.value == answer.value }) {
            answers.remove(answer)
            selectedAnswers.add(answer)
        } else {
            answers.add(answer)
            selectedAnswers.remove(answer)
        }
        updateAnswersSolvingState()
    }

    private fun updateAnswersSolvingState() {
        updateState { state ->
            (state as? SolvingState.Solving)?.copy(
                answers = answers.toList(),
                selectedAnswers = selectedAnswers.toList(),
                time = time.toString()
            ) ?: state
        }
    }

    fun confirmTaskSolved() {
        updateState { state ->
            (state as? SolvingState.Solving)?.let {
                SolvingState.Congratulations(
                    studentName = state.studentName,
                    time = time.toString()
                )
            } ?: state
        }
    }

    fun returnToSolving() {
        updateState { state ->
            (state as? SolvingState.Congratulations)?.let {
                SolvingState.Solving(
                    studentName = state.studentName,
                    question = question?.questionText ?: throw IllegalStateException(),
                    answers = answers.toList(),
                    selectedAnswers = selectedAnswers.toList(),
                    time = time.toString()
                )
            } ?: state
        }
    }
}