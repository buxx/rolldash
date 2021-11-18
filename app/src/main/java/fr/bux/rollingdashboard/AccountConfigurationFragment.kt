package fr.bux.rollingdashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import fr.bux.rollingdashboard.databinding.AccountConfigurationFragmentBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class AccountConfigurationFragment : Fragment() {

    private var _binding: AccountConfigurationFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonGoBackMain.setOnClickListener {
            findNavController().navigate(R.id.action_AccountConfigurationFragment_to_DashboardFragment)
        }

        binding.buttonSave.setOnClickListener {
            Toast.makeText(context, R.string.saving, Toast.LENGTH_LONG).show()

            val server_address = binding.textInputServerAddress.text.toString()
            val user_name = binding.textInputUserName.text.toString()
            val password = binding.passwordPassword.text.toString()
            val notify_hungry = binding.switchNotificateHungry.isChecked
            val notify_thirsty = binding.switchThirst.isChecked
            val notify_ap = binding.switchMaxAp.isChecked
            val network_grab_each = 3600 // FIXME : determine value for real
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}