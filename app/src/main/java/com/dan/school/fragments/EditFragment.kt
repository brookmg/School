package com.dan.school.fragments

import android.animation.ArgbEvaluator
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.DatePicker
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.antonyt.infiniteviewpager.InfinitePagerAdapter
import com.dan.school.*
import com.dan.school.School.categoryButtonAddColorStateList
import com.dan.school.School.categoryButtonAddRippleColorStateList
import com.dan.school.School.categoryCardViewBackgroundColors
import com.dan.school.School.categoryCheckedIcons
import com.dan.school.School.categoryChipBackgroundColorStateList
import com.dan.school.School.categoryChipStrokeColorStateList
import com.dan.school.School.categoryColors
import com.dan.school.School.categoryUncheckedIcons
import com.dan.school.adapters.CategoryViewPagerAdapter
import com.dan.school.adapters.SubtaskListAdapter
import com.dan.school.models.Item
import com.dan.school.models.Subtask
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_edit.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class EditFragment : DialogFragment(), SubtaskListAdapter.SetFocusListener,
    DatePickerDialog.OnDateSetListener,
    DatePickerFragment.OnCancelListener, ConfirmDeleteDialogFragment.ConfirmDeleteListener {

    private lateinit var categoryChangeListener: CategoryChangeListener
    private lateinit var dismissBottomSheetListener: DismissBottomSheetListener

    private var category: Int = School.HOMEWORK
    private var done: Boolean = false
    private var doneTime: Long? = null
    private var timeCreated: Long? = null
    private var title: String = ""
    private var subtasks: ArrayList<Subtask> = ArrayList()
    private var notes: String = ""
    private var chipGroupSelected: Int = School.TODAY
    private var selectedDate: Calendar? = null
    private var isEdit: Boolean = false
    private var itemId: String? = null

    private val dateToday = Calendar.getInstance()
    private val dateTomorrow = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var subtaskListAdapter: SubtaskListAdapter

    private val dataViewModel: DataViewModel by activityViewModels()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations =
            R.style.DialogAnimation
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is OverviewFragment) {
            categoryChangeListener = (parentFragment as OverviewFragment)
            dismissBottomSheetListener = (parentFragment as OverviewFragment)
        }
        inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            R.style.FullScreenDialog
        )

        category = requireArguments().getInt("category", School.HOMEWORK)
        done = requireArguments().getBoolean("done", false)
        doneTime = if (requireArguments().getLong(
                "doneTime",
                -1
            ) == -1L
        ) null else requireArguments().getLong("doneTime")
        timeCreated = if (requireArguments().getLong(
                "timeCreated",
                -1
            ) == -1L
        ) null else requireArguments().getLong("timeCreated")
        title = requireArguments().getString("title", "")
        subtasks =
            requireArguments().getParcelableArrayList<Subtask>("subtasks") as ArrayList<Subtask>
        notes = requireArguments().getString("notes", "")
        chipGroupSelected = requireArguments().getInt("chipGroupSelected", School.TODAY)

        val selectedDateString = requireArguments().getString("selectedDate", "")
        if (selectedDateString == "") {
            selectedDate = null
        } else {
            selectedDate = Calendar.getInstance()
            selectedDate?.time =
                SimpleDateFormat(School.dateFormatOnDatabase, Locale.getDefault()).parse(
                    selectedDateString
                )!!
        }

        isEdit = requireArguments().getBoolean("isEdit", false)
        itemId = requireArguments().getString("itemId", "")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setOnShowListener {
            if (this::dismissBottomSheetListener.isInitialized) {
                dismissBottomSheetListener.dismissBottomSheet()
            }
        }

        val viewPagerAdapter = InfinitePagerAdapter(
            CategoryViewPagerAdapter(
                requireContext()
            )
        )

        viewPagerCategory.adapter = viewPagerAdapter
        viewPagerCategory.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                val realPosition = viewPagerCategory.currentItem
                changeColors(realPosition)
            }
        })

        // listeners
        buttonAddSubtask.setOnClickListener {
            subtaskListAdapter.data.add(Subtask())
            subtaskListAdapter.notifyItemInserted(subtaskListAdapter.itemCount - 1)
            inputMethodManager.toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                0
            )
        }
        chipGroupDate.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipToday -> {
                    textViewDatePicked.text = dateFormat.format(dateToday.time)
                    selectedDate = dateToday
                    chipGroupSelected = School.TODAY
                }
                R.id.chipTomorrow -> {
                    textViewDatePicked.text = dateFormat.format(dateTomorrow.time)
                    selectedDate = dateTomorrow
                    chipGroupSelected = School.TOMORROW
                }
            }
        }
        chipPickDate.setOnClickListener {
            val datePicker: DialogFragment =
                DatePickerFragment(this, this)
            datePicker.show(childFragmentManager, "date picker")
        }
        buttonCheck.setOnClickListener {
            saveData()
        }
        buttonBack.setOnClickListener {
            dismiss()
        }
        buttonDelete.setOnClickListener {
            if (isEdit) {
                itemId?.let { it1 ->
                    ConfirmDeleteDialogFragment(
                        this,
                        it1,
                        title
                    ).show(childFragmentManager, "confirmDeleteDialog")
                }
            }
        }
        buttonFinish.setOnClickListener {
            if (isEdit) {
                val item = Item(
                    id = itemId!!,
                    category = category,
                    done = !done,
                    doneTime = Calendar.getInstance().timeInMillis,
                    timeCreated = timeCreated,
                    title = editTextTitle.text.toString(),
                    date = SimpleDateFormat(
                        School.dateFormatOnDatabase,
                        Locale.getDefault()
                    ).format(selectedDate!!.time).toInt(),
                    subtasks = subtaskListAdapter.data,
                    notes = editTextNotes.text.toString()
                )
                dataViewModel.update(item)
                dismiss()
            }
        }

        // [START] initialize
        dateTomorrow.add(Calendar.DAY_OF_MONTH, 1)
        textViewDatePicked.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                categoryColors[category]
            )
        )
        editTextTitle.setText(title)
        when (chipGroupSelected) {
            School.TODAY -> {
                chipGroupDate.check(R.id.chipToday)
                textViewDatePicked.text = dateFormat.format(dateToday.time)
                selectedDate = dateToday
            }
            School.TOMORROW -> {
                chipGroupDate.check(R.id.chipTomorrow)
                textViewDatePicked.text = dateFormat.format(dateTomorrow.time)
                selectedDate = dateTomorrow
            }
            School.PICK_DATE -> {
                chipGroupDate.check(R.id.chipPickDate)
                textViewDatePicked.text = dateFormat.format(selectedDate!!.time)
            }
        }

        if (category == School.HOMEWORK) {
            changeColors(category)
        } else {
            viewPagerCategory.currentItem += category
        }

        // [START] configure subtasks list
        editTextNotes.setText(notes)
        subtaskListAdapter = SubtaskListAdapter(
            requireContext(),
            this,
            categoryCheckedIcons[category],
            categoryUncheckedIcons[category],
            subtasks,
            !isEdit
        )
        recyclerViewSubtasks.layoutManager = LinearLayoutManager(context)
        recyclerViewSubtasks.adapter = subtaskListAdapter
        // [END] configure subtasks list

        if (!isEdit) {
            editTextTitle.requestFocus()
        }
        linearLayoutBottomButtons.isVisible = isEdit
        cardViewCompleted.isVisible = done.apply {
            if (this && doneTime != null) {
                textViewDateCompleted.text =
                    SimpleDateFormat(School.dateTimeFormat, Locale.getDefault()).format(
                        Calendar.getInstance().apply {
                            timeInMillis = doneTime!!
                        }.time
                    )
            }
        }
        buttonFinish.text =
            if (done) resources.getString(R.string.uncheck) else resources.getString(R.string.check)
        // [END] initialize
    }

    /**
     * Inserts or updates data on database depending
     * on [isEdit] and calls [dismiss] after
     */
    private fun saveData() {
        if (isEdit) {
            if (itemId != null) {
                val item = Item(
                    id = itemId!!,
                    category = category,
                    done = done,
                    doneTime = doneTime,
                    timeCreated = timeCreated,
                    title = editTextTitle.text.toString(),
                    date = SimpleDateFormat(
                        School.dateFormatOnDatabase,
                        Locale.getDefault()
                    ).format(selectedDate!!.time).toInt(),
                    subtasks = subtaskListAdapter.data,
                    notes = editTextNotes.text.toString()
                )
                dataViewModel.update(item)
            }
        } else {
            val item = Item(
                category = category,
                timeCreated = Date().time,
                title = editTextTitle.text.toString(),
                date = SimpleDateFormat(
                    School.dateFormatOnDatabase,
                    Locale.getDefault()
                ).format(selectedDate!!.time).toInt(),
                subtasks = subtaskListAdapter.data,
                notes = editTextNotes.text.toString()
            )
            dataViewModel.insert(item)
        }
        dismiss()
    }

    /**
     * Changes views', not including subtask items,
     * colors depending on the given [newCategory]
     *
     * See [changeSubtaskListColor] for the changing of
     * subtask items' colors
     */
    private fun changeColors(newCategory: Int) {
        val colorFrom = ContextCompat.getColor(requireContext(), categoryColors[category])
        val colorTo = ContextCompat.getColor(requireContext(), categoryColors[newCategory])
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.addUpdateListener { animator ->
            val animatedValue = animator.animatedValue as Int
            textViewDatePicked.setTextColor(animatedValue)
            textViewDatePicked.setTextColor(animatedValue)
        }
        colorAnimation.start()
        chipPickDate.chipBackgroundColor = ContextCompat.getColorStateList(
            requireContext(),
            categoryChipBackgroundColorStateList[newCategory]
        )
        chipToday.chipBackgroundColor = ContextCompat.getColorStateList(
            requireContext(),
            categoryChipBackgroundColorStateList[newCategory]
        )
        chipTomorrow.chipBackgroundColor = ContextCompat.getColorStateList(
            requireContext(),
            categoryChipBackgroundColorStateList[newCategory]
        )
        chipPickDate.chipStrokeColor = ContextCompat.getColorStateList(
            requireContext(),
            categoryChipStrokeColorStateList[newCategory]
        )
        chipToday.chipStrokeColor = ContextCompat.getColorStateList(
            requireContext(),
            categoryChipStrokeColorStateList[newCategory]
        )
        chipTomorrow.chipStrokeColor = ContextCompat.getColorStateList(
            requireContext(),
            categoryChipStrokeColorStateList[newCategory]
        )
        chipTomorrow.chipStrokeColor = ContextCompat.getColorStateList(
            requireContext(),
            categoryChipStrokeColorStateList[newCategory]
        )
        chipTomorrow.chipStrokeColor = ContextCompat.getColorStateList(
            requireContext(),
            categoryChipStrokeColorStateList[newCategory]
        )
        (buttonAddSubtask as MaterialButton).iconTint = ContextCompat.getColorStateList(
            requireContext(),
            categoryButtonAddColorStateList[newCategory]
        )
        buttonAddSubtask.setTextColor(
            ContextCompat.getColorStateList(
                requireContext(),
                categoryButtonAddColorStateList[newCategory]
            )
        )
        (buttonAddSubtask as MaterialButton).rippleColor = ContextCompat.getColorStateList(
            requireContext(),
            categoryButtonAddRippleColorStateList[newCategory]
        )
        (cardViewCompleted as MaterialCardView).setCardBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                categoryCardViewBackgroundColors[newCategory]
            )
        )
        changeSubtaskListColor(newCategory)
        if (this::categoryChangeListener.isInitialized) {
            categoryChangeListener.selectedCategoryChanged(newCategory)
        }
        category = newCategory
    }

    /**
     * Changes subtask items' colors depending on the
     * given [newCategory]
     */
    private fun changeSubtaskListColor(newCategory: Int) {
        if (this::subtaskListAdapter.isInitialized) {
            for (i in 0 until subtaskListAdapter.itemCount) {
                val v = recyclerViewSubtasks.getChildAt(i)
                val buttonCheck = v.findViewById<ImageButton>(R.id.buttonCheck)
                if (subtaskListAdapter.data[i].done) {
                    buttonCheck.setImageResource(categoryCheckedIcons[newCategory])
                } else {
                    buttonCheck.setImageResource(categoryUncheckedIcons[newCategory])
                }
                buttonCheck.setOnClickListener {
                    if (subtaskListAdapter.data[i].done) {
                        buttonCheck.setImageResource(categoryUncheckedIcons[newCategory])
                        subtaskListAdapter.data[i].done = false
                    } else {
                        buttonCheck.setImageResource(categoryCheckedIcons[newCategory])
                        subtaskListAdapter.data[i].done = true
                    }
                }
            }
        }
    }

    override fun setFocus(position: Int) {
        if (position == -1) {
            editTextTitle.requestFocus()
        } else {
            (recyclerViewSubtasks.getChildViewHolder(
                recyclerViewSubtasks.getChildAt(position)
            ) as SubtaskListAdapter.SubtaskViewHolder).editTextSubtaskTitle.requestFocus()
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        if (selectedDate == null) {
            selectedDate = Calendar.getInstance()
        }
        selectedDate!!.set(Calendar.YEAR, year)
        selectedDate!!.set(Calendar.MONTH, month)
        selectedDate!!.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        textViewDatePicked.text = dateFormat.format(selectedDate!!.time)
        chipGroupSelected = School.PICK_DATE
    }

    override fun canceled() {
        when (chipGroupSelected) {
            School.TODAY -> chipGroupDate.check(R.id.chipToday)
            School.TOMORROW -> chipGroupDate.check(R.id.chipTomorrow)
            School.PICK_DATE -> chipGroupDate.check(R.id.chipPickDate)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        super.onDismiss(dialog)
    }

    interface DismissBottomSheetListener {
        fun dismissBottomSheet()
    }

    interface CategoryChangeListener {
        fun selectedCategoryChanged(category: Int)
    }

    companion object {
        fun newInstance(
            category: Int = School.HOMEWORK,
            done: Boolean = false,
            doneTime: Long? = null,
            timeCreated: Long? = null,
            title: String = "",
            subtasks: ArrayList<Subtask> = ArrayList(),
            notes: String = "",
            chipGroupSelected: Int = School.TODAY,
            selectedDate: Calendar? = null,
            isEdit: Boolean = false,
            itemId: String? = null
        ) = EditFragment().apply {
            arguments = bundleOf(
                "category" to category,
                "done" to done,
                "doneTime" to doneTime,
                "timeCreated" to timeCreated,
                "title" to title,
                "subtasks" to subtasks,
                "notes" to notes,
                "chipGroupSelected" to chipGroupSelected,
                "selectedDate" to if (selectedDate == null) "" else SimpleDateFormat(
                    School.dateFormatOnDatabase,
                    Locale.getDefault()
                ).format(
                    selectedDate.time
                ),
                "isEdit" to isEdit,
                "itemId" to itemId
            )
        }
    }

    override fun confirmDelete(itemId: String) {
        dataViewModel.deleteItemWithId(itemId)
        dismiss()
    }
}
