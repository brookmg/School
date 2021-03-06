package com.dan.school.settings

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dan.school.*
import com.dan.school.adapters.BackupListAdapter
import com.dan.school.authentication.AuthenticationActivity
import com.dan.school.databinding.FragmentBackupBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class BackupFragment : Fragment(), BackupItemClickListener,
    BackupListAdapter.BackupItemLongClickListener {

    private var _binding: FragmentBackupBinding? = null

    private val binding get() = _binding!!

    private val dataViewModel: DataViewModel by activityViewModels()

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage

    private lateinit var backupListAdapter: BackupListAdapter

    private var restoringDatabase = false

    private lateinit var progressBarDialog: ProgressBarDialog

    private lateinit var settingsGoToFragmentListener: SettingsGoToFragmentListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is SettingsActivity) {
            settingsGoToFragmentListener = activity as SettingsActivity
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        progressBarDialog = ProgressBarDialog(requireContext())

        auth = Firebase.auth
        storage = Firebase.storage

        val timeout: Long = 10000
        storage.maxOperationRetryTimeMillis = timeout
        storage.maxDownloadRetryTimeMillis = timeout
        storage.maxUploadRetryTimeMillis = timeout
    }

    override fun onResume() {
        super.onResume()

        binding.swipeRefreshLayout.isRefreshing = true
        check()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backupListAdapter = BackupListAdapter(
            requireContext(),
            this@BackupFragment,
            this@BackupFragment
        )

        binding.recyclerViewBackups.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = backupListAdapter
        }

        binding.buttonCreateBackup.setOnClickListener {
            if (isNetworkAvailable(requireContext())) {
                showProgressBar()
                dataViewModel.checkpoint()
                backup { successful ->
                    if (successful) {
                        updateBackupList {
                            showDialog(
                                getString(R.string.backup_created_successfully),
                                getString(R.string.backup_successful)
                            )
                        }
                    } else {
                        showDialog(
                            getString(R.string.error_while_performing_backup),
                            getString(R.string.backup_failed)
                        )
                        hideProgressBar()
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.no_internet),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.buttonRetry.setOnClickListener {
            binding.swipeRefreshLayout.isRefreshing = true
            swipeRefreshUpdate()
        }

        binding.buttonSignIn.setOnClickListener {
            val intent = Intent(requireContext(), AuthenticationActivity::class.java)
            intent.putExtra(School.SHOW_BUTTON_SIGN_IN_LATER, false)
            startActivity(intent)
        }

        binding.buttonProfile.setOnClickListener {
            settingsGoToFragmentListener.goToFragment(School.PROFILE)
        }

        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshUpdate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun swipeRefreshUpdate() {
        check()
    }

    private fun check() {
        if (isNetworkAvailable(requireContext())) {
            val user = auth.currentUser
            if (user != null) {

                if (user.isEmailVerified) {
                    updateBackupList {
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    return
                }

                user.getIdToken(true).addOnCompleteListener { taskGetIdToken ->
                    if (taskGetIdToken.isSuccessful) {
                        user.reload().addOnCompleteListener reload@{ taskReload ->
                            if (_binding == null) {
                                return@reload
                            }
                            if (taskReload.isSuccessful) {
                                auth.currentUser?.let {
                                    if (it.isEmailVerified) {
                                        updateBackupList {
                                            binding.swipeRefreshLayout.isRefreshing = false
                                        }
                                    } else {
                                        binding.groupBackupLayout.visibility = View.GONE
                                        binding.groupInternetRequired.visibility = View.GONE
                                        binding.groupAccountRequired.visibility = View.GONE
                                        binding.groupVerificationRequired.visibility = View.VISIBLE
                                        binding.swipeRefreshLayout.isRefreshing = false
                                    }
                                }
                            } else {
                                try {
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.error_while_getting_list_of_backups),
                                        Toast.LENGTH_LONG
                                    ).show()
                                } catch (e: Exception) {
                                }
                                binding.swipeRefreshLayout.isRefreshing = false
                            }
                        }
                    } else {
                        if (_binding == null) {
                            return@addOnCompleteListener
                        }
                        try {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.error_while_getting_list_of_backups),
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                        }
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
            } else {
                binding.groupBackupLayout.visibility = View.GONE
                binding.groupInternetRequired.visibility = View.GONE
                binding.groupAccountRequired.visibility = View.VISIBLE
                binding.groupVerificationRequired.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        } else {
            binding.groupBackupLayout.visibility = View.GONE
            binding.groupInternetRequired.visibility = View.VISIBLE
            binding.groupAccountRequired.visibility = View.GONE
            binding.groupVerificationRequired.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun updateBackupList(done: () -> Unit) {
        storage.reference.child(School.USERS).child(auth.currentUser!!.uid)
            .listAll()
            .addOnCompleteListener {
                if (_binding == null) {
                    return@addOnCompleteListener
                }
                if (it.isSuccessful) {
                    val backupList = ArrayList<StorageReference>()
                    if (it.result != null) {
                        it.result!!.items.forEach { item ->
                            backupList.add(item)
                        }
                    }

                    backupList.sortWith { o1, o2 ->
                        o2.name.compareTo(o1.name)
                    }

                    binding.groupBackupLayout.visibility = View.VISIBLE
                    binding.groupInternetRequired.visibility = View.GONE
                    binding.groupAccountRequired.visibility = View.GONE
                    binding.groupVerificationRequired.visibility = View.GONE

                    binding.textViewNoBackupsYet.isGone = backupList.isNotEmpty()
                    binding.recyclerViewBackups.isVisible = backupList.isNotEmpty()
                    backupListAdapter.submitList(backupList)
                } else {
                    try {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_while_getting_list_of_backups),
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                    }
                }
                done()
                hideProgressBar()
            }
    }

    private fun showProgressBar() {
        progressBarDialog.show()
    }

    private fun hideProgressBar() {
        progressBarDialog.dismiss()
    }

    private fun backup(
        isBeforeRestore: Boolean = false,
        backupComplete: (success: Boolean) -> Unit
    ) {
        val dbFile: File = requireContext().getDatabasePath(School.DATABASE_NAME)
        val fileName: String =
            "BACKUP_" + SimpleDateFormat(School.dateFormatOnBackupFile, Locale.getDefault()).format(
                Calendar.getInstance().time
            )
        try {
            val inputStream: InputStream = FileInputStream(dbFile)

            // Check if file is less than 2MB
            if (inputStream.available() < 2 * 1024 * 1024) {

                // Check if the number of backups is less than 10
                val ref = storage.reference.child(School.USERS).child(auth.currentUser!!.uid)
                ref.listAll().addOnCompleteListener { taskListAll ->
                    if (taskListAll.isSuccessful) {
                        if (taskListAll.result.items.size < 10) {
                            storage.reference.child(School.USERS).child(auth.currentUser!!.uid)
                                .child(fileName)
                                .putStream(inputStream).addOnCompleteListener {
                                    backupComplete(it.isSuccessful)
                                }
                        } else {
                            showDialog(
                                "You are only allowed to have 10 backups, you currently have ${taskListAll.result.items.size}. Please delete some backups first to create a new one.",
                                "Delete some backups"
                            ) {
                                if (isBeforeRestore) {
                                    showRestoreCancelled()
                                }
                            }
                            hideProgressBar()
                        }
                    } else {
                        showDialog(
                            getString(R.string.error_while_performing_backup),
                            getString(R.string.backup_failed)
                        ) {
                            if (isBeforeRestore) {
                                showRestoreCancelled()
                            }
                        }
                        hideProgressBar()
                    }
                }
            } else {
                showDialog(
                    getString(R.string.file_is_too_big_message),
                    getString(R.string.file_is_too_big)
                ) {
                    if (isBeforeRestore) {
                        showRestoreCancelled()
                    }
                }
                hideProgressBar()
            }
        } catch (e: Exception) {
            showDialog(
                getString(R.string.error_while_performing_backup),
                getString(R.string.backup_failed)
            ) {
                if (isBeforeRestore) {
                    showRestoreCancelled()
                }
            }
            hideProgressBar()
        }
    }

    private fun showRestoreCancelled() {
        showDialog(
            null,
            getString(R.string.restore_cancelled)
        )
        restoringDatabase = false
    }

    private fun restore(storageReference: StorageReference, backupCurrentDatabase: Boolean) {
        if (backupCurrentDatabase) {
            backup(true) { success ->
                if (success) {
                    getFile(storageReference, { byteArray ->
                        restoreDatabase(byteArray)
                    }, {
                        showDialog(
                            getString(R.string.error_while_performing_restore),
                            getString(R.string.restore_failed)
                        )
                        hideProgressBar()
                        restoringDatabase = false
                    })
                } else {
                    showDialog(
                        getString(R.string.error_restore_cancelled),
                        getString(R.string.restore_failed)
                    )
                    hideProgressBar()
                    restoringDatabase = false
                }
            }
        } else {
            getFile(storageReference, { byteArray ->
                restoreDatabase(byteArray)
            }, {
                showDialog(
                    getString(R.string.error_while_performing_restore),
                    getString(R.string.restore_failed)
                )
                hideProgressBar()
                restoringDatabase = false
            })
        }
    }

    private fun restoreDatabase(byteArray: ByteArray) {
        ItemDatabase.getInstance(requireContext()).close()

        val oldDB: File = requireContext().getDatabasePath(School.DATABASE_NAME)
        try {
            FileOutputStream(oldDB).use { fos -> fos.write(byteArray) }

            MaterialAlertDialogBuilder(requireContext()).setMessage(getString(R.string.restore_successful_restart_app))
                .setTitle(getString(R.string.restore_successful))
                .setPositiveButton(
                    getString(R.string.restart)
                ) { _, _ -> }
                .setOnDismissListener {
                    restart()
                }
                .create()
                .show()
        } catch (e: IOException) {
            showDialog(
                getString(R.string.error_while_performing_restore),
                getString(R.string.restore_failed)
            )
            hideProgressBar()
        }
    }

    private fun getFile(
        storageReference: StorageReference,
        success: (byteArray: ByteArray) -> Unit,
        failed: (e: Exception) -> Unit
    ) {
        val twoMegabytes: Long = 2 * 1024 * 1024
        storageReference.getBytes(twoMegabytes).addOnSuccessListener {
            success(it)
        }.addOnFailureListener {
            failed(it)
        }
    }

    private fun restart() {
        val pm = requireActivity().packageManager
        val intent = pm.getLaunchIntentForPackage(requireActivity().packageName)
        requireActivity().finishAffinity()
        requireActivity().startActivity(intent)
        exitProcess(0)
    }

    private fun showDialog(message: String?, title: String, dismissed: () -> Unit = {}) {
        try {
            MaterialAlertDialogBuilder(requireContext()).setMessage(message)
                .setTitle(title)
                .setPositiveButton(
                    getString(R.string.okay)
                ) { _, _ -> }
                .setOnDismissListener {
                    dismissed()
                }
                .create()
                .show()
        } catch (e: Exception) {
        }
    }

    private fun askForConfirmation(confirm: () -> Unit) {
        showConfirmationDialog { result ->
            when (result) {
                CONFIRM -> {
                    confirm()
                }
                CANCEL -> {
                    hideProgressBar()
                    restoringDatabase = false
                }
            }
        }
    }

    private fun showConfirmationDialog(done: (result: Int) -> Unit) {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_confirm_dialog, LinearLayout(requireContext()), false)
        val code = ((Math.random() * 9000) + 1000).toInt()
        view.findViewById<TextView>(R.id.textViewCode).text = code.toString()
        val editTextCode = view.findViewById<TextInputLayout>(R.id.editTextCode).editText
        MaterialAlertDialogBuilder(requireContext()).setMessage(getString(R.string.enter_code_to_confirm_restore))
            .setTitle(getString(R.string.confirm_restore))
            .setView(view)
            .setPositiveButton(
                getString(R.string.done)
            ) { _, _ ->
                if (editTextCode != null) {
                    if (editTextCode.text.toString() == code.toString()) {
                        done(CONFIRM)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.code_not_match_restore_cancelled),
                            Toast.LENGTH_LONG
                        ).show()
                        done(CANCEL)
                    }
                } else {
                    done(CANCEL)
                }
            }
            .setNeutralButton(
                getString(R.string.cancel)
            ) { _, _ ->
                done(CANCEL)
            }
            .create()
            .show()
    }

    @Suppress("DEPRECATION")
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }

    override fun backupItemClicked(storageReference: StorageReference) {
        if (isNetworkAvailable(requireContext())) {
            if (!restoringDatabase) {
                restoringDatabase = true
                showProgressBar()
                MaterialAlertDialogBuilder(requireContext()).setMessage(getString(R.string.do_you_want_to_backup_current_database))
                    .setTitle(getString(R.string.backup_current_database))
                    .setNeutralButton(
                        getString(R.string.cancel)
                    ) { _, _ ->
                        hideProgressBar()
                        restoringDatabase = false
                    }
                    .setPositiveButton(
                        R.string.yes
                    ) { _, _ ->
                        askForConfirmation {
                            restore(storageReference, true)
                        }
                    }
                    .setNegativeButton(
                        getString(R.string.no)
                    ) { _, _ ->
                        askForConfirmation {
                            restore(storageReference, false)
                        }
                    }
                    .setOnCancelListener {
                        hideProgressBar()
                        restoringDatabase = false
                    }
                    .create()
                    .show()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.no_internet), Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun backupItemLongClicked(storageReference: StorageReference) {
        showProgressBar()
        MaterialAlertDialogBuilder(requireContext())
            .setMessage("${getString(R.string.are_you_sure_you_want_to_delete_this_backup)} (${storageReference.name})?")
            .setTitle(getString(R.string.delete_backup))
            .setPositiveButton(
                R.string.yes
            ) { _, _ ->
                storageReference.delete().addOnCompleteListener {
                    if (it.isSuccessful) {
                        updateBackupList {}
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.failed_to_delete_backup),
                            Toast.LENGTH_LONG
                        ).show()
                        hideProgressBar()
                    }
                }
            }
            .setNegativeButton(
                getString(R.string.no)
            ) { _, _ ->
                hideProgressBar()
            }
            .setOnCancelListener {
                hideProgressBar()
            }
            .create()
            .show()
    }

    interface SettingsGoToFragmentListener {
        fun goToFragment(fragment: Int)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            BackupFragment()

        const val CONFIRM = 0
        const val CANCEL = 1
    }
}