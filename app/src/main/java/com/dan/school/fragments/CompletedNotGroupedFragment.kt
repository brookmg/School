package com.dan.school.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dan.school.*
import com.dan.school.adapters.ItemListAdapter
import com.dan.school.models.Item
import com.dan.school.models.Subtask
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_completed_not_grouped.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CompletedNotGroupedFragment : Fragment(), ItemListAdapter.DoneListener,
    ItemListAdapter.ShowSubtasksListener, ItemClickListener, ItemListAdapter.ItemLongClickListener,
    ConfirmDeleteDialog.ConfirmDeleteListener {

    private lateinit var completedNotGroupedListAdapter: ItemListAdapter

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataViewModel = ViewModelProvider(this).get(DataViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_completed_not_grouped, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        completedNotGroupedListAdapter = ItemListAdapter(
            requireContext(),
            this,
            this,
            this,
            this
        )

        recyclerViewCompletedNotGrouped.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = completedNotGroupedListAdapter
        }

        dataViewModel.getAllDoneItems().observe(viewLifecycleOwner, androidx.lifecycle.Observer { overdueItems ->
            completedNotGroupedListAdapter.submitList(overdueItems)
        })
    }

    override fun setDone(id: Int, done: Boolean, doneTime: Long?) {
        dataViewModel.setDone(id, done, doneTime)
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
        val calendar = Calendar.getInstance()
        calendar.time = SimpleDateFormat(
            School.dateFormatOnDatabase,
            Locale.getDefault()
        ).parse(item.date.toString())!!
        showEditFragment(
            item.category,
            item.done,
            item.title,
            Gson().fromJson(item.subtasks, object : TypeToken<java.util.ArrayList<Subtask?>?>() {}.type),
            item.notes,
            calendar,
            item.id
        )
    }

    override fun itemLongClicked(title: String, id: Int) {
        ConfirmDeleteDialog(this, id, title).show(childFragmentManager, "confirmDeleteDialog")
    }

    override fun confirmDelete(itemId: Int) {
        dataViewModel.deleteItemWithId(itemId)
    }

    private fun showEditFragment(
        category: Int,
        done: Boolean,
        title: String,
        subtasks: java.util.ArrayList<Subtask>,
        notes: String,
        date: Calendar?,
        itemId: Int
    ) {
        val editFragment = EditFragment.newInstance(
            category = category,
            done = done,
            title = title,
            subtasks = subtasks,
            notes = notes,
            chipGroupSelected = School.PICK_DATE,
            selectedDate = date,
            isEdit = true,
            itemId = itemId
        )
        editFragment.show(childFragmentManager, "editFragment")
    }
}