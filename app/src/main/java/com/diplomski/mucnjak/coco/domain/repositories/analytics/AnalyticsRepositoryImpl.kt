package com.diplomski.mucnjak.coco.domain.repositories.analytics

import com.diplomski.mucnjak.coco.data.domain.ResultsDomainModel
import com.diplomski.mucnjak.coco.data.domain.StudentResultsDomainModel
import com.diplomski.mucnjak.coco.domain.interactor.post_analytics_results.PostAnalyticsResultsInteractor
import com.diplomski.mucnjak.coco.domain.mapper.analytics.AnalyticsMappers
import com.diplomski.mucnjak.coco.domain.repositories.active_activity.ActiveActivityRepository
import com.diplomski.mucnjak.coco.domain.repositories.answer_checker.AnswerCheckerRepository
import com.diplomski.mucnjak.coco.domain.repositories.clock.ClockRepository
import com.diplomski.mucnjak.coco.domain.repositories.iteration.IterationRepository
import com.diplomski.mucnjak.coco.domain.repositories.students.StudentRepository
import com.google.firebase.Timestamp
import javax.inject.Inject

class AnalyticsRepositoryImpl @Inject constructor(
    private val activeActivityRepository: ActiveActivityRepository,
    private val studentRepository: StudentRepository,
    private val answerCheckerRepository: AnswerCheckerRepository,
    private val clockRepository: ClockRepository,
    private val postAnalyticsResultsInteractor: PostAnalyticsResultsInteractor,
    private val analyticsNetworkMapper: AnalyticsMappers.AnalyticsNetworkMapper,
    private val iterationRepository: IterationRepository,
) : AnalyticsRepository {

    private lateinit var results: ResultsDomainModel

    private val studentResults
        get() = results.results

    private val discussionTimes
        get() = results.discussionTimes

    override fun init() {
        results = ResultsDomainModel(
            date = Timestamp.now(),
            group = 0,
            subtopic = activeActivityRepository.getLocalActiveActivity()?.subTopic
                ?: throw NullPointerException(),
            topic = activeActivityRepository.getLocalActiveActivity()?.topic
                ?: throw NullPointerException(),
            discussionTimes = mutableListOf(),
            results = mutableListOf(),
        )
        studentRepository.getAllStudents().forEach { (name, position, _) ->
            studentResults.add(
                StudentResultsDomainModel(
                    studentIndex = position,
                    name = name,
                    accuracies = mutableListOf(),
                    initialResolutionTimes = mutableListOf(),
                )
            )
        }
    }

    override fun calculateAndStoreAccuracies() {
        studentResults.forEach { (studentIndex, _, accuracies, _) ->
            accuracies.add(answerCheckerRepository.getStudentAccuracy(studentIndex))
        }
    }

    override fun storeDiscussionTime() {
        discussionTimes.add(clockRepository.getElapsedTime())
    }

    override fun storeResolutionChangeTime(studentIndex: Int) {
        val iteration = iterationRepository.getCurrentIteration()
        with(studentResults.first { (index, _, _, _) -> index == studentIndex }.initialResolutionTimes) {
            if (size > iteration) {
                get(iteration).add(clockRepository.getElapsedTime())
            } else {
                add(mutableListOf(clockRepository.getElapsedTime()))
            }
        }
    }

    override fun storeResolutionTimeout() {
        val iteration = iterationRepository.getCurrentIteration()
        studentResults.forEach {
            if (it.initialResolutionTimes.size > iteration) {
                it.initialResolutionTimes[iteration].add(clockRepository.getElapsedTime())
            } else {
                it.initialResolutionTimes.add(mutableListOf(clockRepository.getElapsedTime()))
            }
        }
    }

    override suspend fun postAnalytics() {
        postAnalyticsResultsInteractor.postAnalyticsResults(
            analyticsNetworkMapper.mapToNetworkModel(
                results
            )
        )
    }
}