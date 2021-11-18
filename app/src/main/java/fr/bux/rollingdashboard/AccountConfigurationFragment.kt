package fr.bux.rollingdashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonGoBackMain.setOnClickListener {
            findNavController().navigate(R.id.action_AccountConfigurationFragment_to_DashboardFragment)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}