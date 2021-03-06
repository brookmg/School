package com.dan.school.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.dan.school.R
import com.dan.school.School
import com.dan.school.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity(), SettingsFragment.SettingsItemOnClickListener,
    BackupFragment.SettingsGoToFragmentListener {

    private lateinit var binding: ActivitySettingsBinding

    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(
                    R.id.frameLayoutSettings,
                    SettingsFragment.newInstance()
                ).commit()
        }
    }

    override fun itemClicked(item: Int) {
        when (item) {
            School.PROFILE -> {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    .replace(
                        R.id.frameLayoutSettings,
                        ProfileFragment()
                    ).addToBackStack(null)
                    .commit()
            }
            School.BACKUP -> {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    .replace(
                        R.id.frameLayoutSettings,
                        BackupFragment()
                    ).addToBackStack(null)
                    .commit()
            }
            School.ABOUT -> {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    .replace(
                        R.id.frameLayoutSettings,
                        AboutFragment()
                    ).addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun goToFragment(fragment: Int) {
        when (fragment) {
            School.PROFILE -> {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    .replace(
                        R.id.frameLayoutSettings,
                        ProfileFragment()
                    ).addToBackStack(null)
                    .commit()
            }
        }
    }
}