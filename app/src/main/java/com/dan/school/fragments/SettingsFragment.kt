package com.dan.school.fragments

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.DialogFragment
import com.dan.school.MainActivity
import com.dan.school.R
import com.dan.school.School
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : DialogFragment() {

    private lateinit var sharedPref: SharedPreferences

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sharedPref = context.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations =
            R.style.DialogAnimation
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(
            STYLE_NORMAL,
            R.style.FullScreenDialog
        )

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonBack.setOnClickListener {
            dismiss()
        }
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setDarkMode(true)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                setDarkMode(false)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (activity is MainActivity) {
            (activity as MainActivity).onDismiss(dialog)
        }
        super.onDismiss(dialog)
    }

    private fun setDarkMode(isDarkMode: Boolean) {
        with(sharedPref.edit()) {
            this?.putBoolean(School.IS_DARK_MODE, isDarkMode)
            this?.commit()
        }
    }
}