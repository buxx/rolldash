package fr.bux.rollingdashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import fr.bux.rollingdashboard.databinding.DashboardFragmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.time.Duration

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DashboardFragment : Fragment() {
    private var _binding: DashboardFragmentBinding? = null
    lateinit var mainHandler: Handler
    private val binding get() = _binding!!
    private val viewModel: CharacterViewModel by activityViewModels {
        CharacterViewModelFactory(
            (activity?.application as RollingDashboardApplication).character_repository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DashboardFragmentBinding.inflate(inflater, container, false)

        mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(refreshCharacterData)

        return binding.root
    }

    private val refreshCharacterData = object : Runnable {
        override fun run() {

            lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    val character = viewModel.get()
                    if (character != null) {
                        println("Update character data")

                        val lastRefreshDate = Date(character.last_refresh)
                        val currentDate = getCurrentDateTime()
                        val since = getSinceString(currentDate, lastRefreshDate)

                        runOnUiThread {
                            binding.textviewFirst.text = getString(R.string.last_refresh, since)
                            binding.textViewCharacterName.text = getString(R.string.character_name, character.name)
                            val hungry = if (character.hungry) { "Oui" } else { "Non" }
                            binding.textViewCharacterHungry.text = getString(R.string.character_hungry, hungry)
                            val thirsty = if (character.hungry) { "Oui" } else { "Non" }
                            binding.textViewCharacterThirsty.text = getString(R.string.character_thirsty, thirsty)
                            binding.textViewCharacterAp.text = getString(R.string.character_ap, character.action_points.toString())
                        }

                    } else {
                        println("No character data, don't update")
                    }

                }
            }

            // FIXME BS NOW : increase delay
            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_DashboardFragment_to_AccountConfigurationFragment)
        }

        binding.buttonHello.setOnClickListener {
            Toast.makeText(context, "toto2", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}