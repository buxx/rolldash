package fr.bux.rollingdashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import fr.bux.rollingdashboard.databinding.DashboardFragmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.Observer
import java.util.*

const val REFRESH_CHARACTER_DATA_DELAY = 60_000L

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DashboardFragment : Fragment() {
    private var _binding: DashboardFragmentBinding? = null
    lateinit var mainHandler: Handler
    private val binding get() = _binding!!
    // FIXME BS : real live data from this model (instant refresh when worker update or error)
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

        viewModel.character.observe(viewLifecycleOwner, Observer { character ->
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
                runOnUiThread {
                    binding.textviewFirst.text = getString(R.string.need_configure)
                }
            }
        })


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}