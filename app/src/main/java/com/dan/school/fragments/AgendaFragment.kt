package com.dan.school.fragments

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dan.school.DataViewModel
import com.dan.school.ItemClickListener
import com.dan.school.R
import com.dan.school.School
import com.dan.school.adapters.ItemListAdapter
import com.dan.school.models.Item
import com.dan.school.models.Subtask
import kotlinx.android.synthetic.main.fragment_agenda.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AgendaFragment(private val itemClickListener: ItemClickListener) : Fragment(),
    ItemListAdapter.DoneListener,
    ItemListAdapter.ShowSubtasksListener, ItemClickListener {

    private lateinit var dataViewModel: DataViewModel

    private val categoryCheckedIcons = arrayOf(
        R.drawable.ic_homework_checked,
        R.drawable.ic_exam_checked,
        R.drawable.ic_task_checked
    )
    private val categoryUncheckedIcons = arrayOf(
        R.drawable.ic_homework_unchecked,
        R.drawable.ic_exam_unchecked,
        R.drawable.ic_task_unchecked
    )

    private val dateToday = Calendar.getInstance()
    private val hourOfDay = dateToday.get(Calendar.HOUR_OF_DAY)
    private val userName = "Dan"

    private lateinit var homeworkListAdapter: ItemListAdapter
    private lateinit var examListAdapter: ItemListAdapter
    private lateinit var taskListAdapter: ItemListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataViewModel = ViewModelProvider(this).get(DataViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_agenda, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewGreeting.text = when (hourOfDay) {
            in 5..11 -> {
                "Good Morning, $userName!"
            }
            in 12..16 -> {
                "Good Afternoon, $userName!"
            }
            else -> {
                "Good Evening, $userName!"
            }
        }

        homeworkListAdapter = ItemListAdapter(
            requireContext(),
            this,
            this,
            this,
            categoryUncheckedIcons[School.HOMEWORK],
            categoryCheckedIcons[School.HOMEWORK]
        )
        examListAdapter = ItemListAdapter(
            requireContext(),
            this,
            this,
            this,
            categoryUncheckedIcons[School.EXAM],
            categoryCheckedIcons[School.EXAM]
        )
        taskListAdapter = ItemListAdapter(
            requireContext(),
            this,
            this,
            this,
            categoryUncheckedIcons[School.TASK],
            categoryCheckedIcons[School.TASK]
        )

        recyclerViewHomeworks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = homeworkListAdapter
        }
        recyclerViewExams.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = examListAdapter
        }
        recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskListAdapter
        }

        textViewDate.text =
            SimpleDateFormat(School.displayDateFormat, Locale.getDefault()).format(dateToday.time)

        dataViewModel.getAllHomeworkByDate(
            SimpleDateFormat(
                School.dateFormat,
                Locale.getDefault()
            ).format(dateToday.time)
        ).observe(viewLifecycleOwner, androidx.lifecycle.Observer { homeworks ->
            if (homeworks.isEmpty()) {
                groupHomework.visibility = View.GONE
            } else {
                groupHomework.visibility = View.VISIBLE
            }
            homeworkListAdapter.submitList(homeworks)
        })

        dataViewModel.getAllExamByDate(
            SimpleDateFormat(
                School.dateFormat,
                Locale.getDefault()
            ).format(dateToday.time)
        ).observe(viewLifecycleOwner, androidx.lifecycle.Observer { exams ->
            if (exams.isEmpty()) {
                groupExam.visibility = View.GONE
            } else {
                groupExam.visibility = View.VISIBLE
            }
            examListAdapter.submitList(exams)
        })

        constraintLayoutAgendaMain.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        dataViewModel.getAllTaskByDate(
            SimpleDateFormat(
                School.dateFormat,
                Locale.getDefault()
            ).format(dateToday.time)
        ).observe(viewLifecycleOwner, androidx.lifecycle.Observer { tasks ->
            if (tasks.isEmpty()) {
                groupTask.visibility = View.GONE
            } else {
                groupTask.visibility = View.VISIBLE
            }
            taskListAdapter.submitList(tasks)
        })
    }

    override fun setDone(id: Int, done: Boolean) {
        dataViewModel.setDone(id, done)
    }

    override fun showSubtasks(
        subtasks: ArrayList<Subtask>,
        itemTitle: String,
        id: Int,
        category: Int
    ) {
        SubtasksBottomSheetDialogFragment(
            subtasks,
            itemTitle,
            id,
            categoryUncheckedIcons[category],
            categoryCheckedIcons[category]
        ).show(
            childFragmentManager,
            "subtasksBottomSheet"
        )
    }

    override fun itemClicked(item: Item) {
        itemClickListener.itemClicked(item)
    }
}