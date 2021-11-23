package fr.bux.rollingdashboard

import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.WorkManager
import fr.bux.rollingdashboard.databinding.AccountConfigurationFragmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)
fun Fragment?.runOnUiThread(action: () -> Unit) {
    this ?: return
    if (!isAdded) return // Fragment not attached to an Activity
    activity?.runOnUiThread(action)
}

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class AccountConfigurationFragment : Fragment() {

    private var _binding: AccountConfigurationFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: AccountConfigurationViewModel by activityViewModels {
        AccountConfigurationViewModelFactory(
            (activity?.application as RollingDashboardApplication).account_configuration_repository
        )
    }
    lateinit var accountConfiguration: AccountConfiguration

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = AccountConfigurationFragmentBinding.inflate(inflater, container, false)

        // Set text input hints
        binding.textInputServerAddress.setHint(R.string.text_input_server_address_hint)
        binding.textInputUserName.setHint(R.string.text_input_user_name_hint)
        binding.passwordPassword.setHint(R.string.text_input_password_hint)

        // Fill the spinner
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.network_grab_period_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.spinnerNetworkGrabPeriod.adapter = adapter
        }

        return binding.root

    }

    private fun isEntryValid(): Boolean {
        val networkGrabEach = 3600 // FIXME : determine value for real
        return viewModel.isEntryValid(
            binding.textInputServerAddress.text.toString(),
            binding.textInputUserName.text.toString(),
            binding.passwordPassword.text.toString(),
            networkGrabEach,
        )
    }

    private fun save() {
        if (isEntryValid()) {
            Toast.makeText(context, R.string.saving, Toast.LENGTH_LONG).show()

            // Determine if it is the first account configuration
            var itsFirst = true
            lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    val accountConfiguration = viewModel.get()
                    itsFirst = accountConfiguration != null
                }
            }

            println("Insert account configuration in database")
            val networkGrabEach = 3600 // FIXME : determine value for real
            viewModel.insert(
                AccountConfiguration(
                    server_address = binding.textInputServerAddress.text.toString(),
                    user_name = binding.textInputUserName.text.toString(),
                    password = binding.passwordPassword.text.toString(),
                    notify_hungry = binding.switchNotificateHungry.isChecked,
                    notify_thirsty = binding.switchThirst.isChecked,
                    notify_ap = binding.switchMaxAp.isChecked,
                    network_grab_each = networkGrabEach,
                )
            )

            if (itsFirst) {
                println("It is the first account configuration, enqueue work request")
                val workManager = WorkManager.getInstance(requireContext())
                workManager.enqueue(
                    (activity?.application  as RollingDashboardApplication).buildPeriodicGrabCharacterWorkRequest()
                )
            } else {
                println("It is not the first account configuration, require immediate grab")
                // FIXME BS NOW : Do it, but ... how ? :)
            }

            Toast.makeText(context, R.string.account_configuration_saved, Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_AccountConfigurationFragment_to_DashboardFragment)
        } else {
            Toast.makeText(context, R.string.wrong_inputs, Toast.LENGTH_LONG).show()
        }

    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonGoBackMain.setOnClickListener {
            findNavController().navigate(R.id.action_AccountConfigurationFragment_to_DashboardFragment)
        }

        // Fill form with existing account configuration
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                val accountConfiguration = viewModel.get();

                if (accountConfiguration != null) {
                    runOnUiThread {
                        binding.textInputServerAddress.text = accountConfiguration.server_address.toEditable()
                        binding.textInputUserName.text = accountConfiguration.user_name.toEditable()
                        binding.passwordPassword.text = accountConfiguration.password.toEditable()
                        binding.switchNotificateHungry.isChecked = accountConfiguration.notify_hungry
                        binding.switchThirst.isChecked = accountConfiguration.notify_thirsty
                        binding.switchMaxAp.isChecked = accountConfiguration.notify_ap
                    }

                }
            }
        }

        binding.buttonSave.setOnClickListener {
            save()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}