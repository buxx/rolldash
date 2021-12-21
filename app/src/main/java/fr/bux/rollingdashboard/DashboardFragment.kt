package fr.bux.rollingdashboard

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import fr.bux.rollingdashboard.databinding.DashboardFragmentBinding
import java.util.*

import android.graphics.BitmapFactory

import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


const val REFRESH_CHARACTER_DATA_DELAY = 60_000L

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DashboardFragment : Fragment() {
    private var _binding: DashboardFragmentBinding? = null
    lateinit var mainHandler: Handler
    private val binding get() = _binding!!

    private val characterViewModel: CharacterViewModel by activityViewModels {
        CharacterViewModelFactory(
            (activity?.application as RollingDashboardApplication).character_repository
        )
    }
    private val systemDataViewModel: SystemDataViewModel by activityViewModels {
        SystemDataViewModelFactory(
            (activity?.application as RollingDashboardApplication).system_data_repository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DashboardFragmentBinding.inflate(inflater, container, false)

        characterViewModel.character.observe(viewLifecycleOwner, { character ->
            if (character != null) {
                println("Update character data")

                // Try to display avatar if file exist
                val applicationDir = activity?.applicationInfo?.dataDir
                if ( applicationDir != null) {
                    val fileName = "$applicationDir/avatar.png"
                    val imgFile = File(fileName)
                    if (imgFile.exists()) {
                        val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        val myImage: ImageView = binding.avatarView
                        myImage.setImageBitmap(myBitmap)
                    } else {
                        println("no avatar")
                    }
                } else {
                    println("Fail to retrieve application data dir because no activity")
                }

                val lastRefreshDate = Date(character.last_refresh)
                val currentDate = getCurrentDateTime()
                val since = getSinceString(currentDate, lastRefreshDate)

                runOnUiThread {
                    if (character.alive) {
                        binding.textviewFirst.text = getString(R.string.last_refresh, since)
                        binding.textViewCharacterName.text = getString(R.string.character_name, character.name)
                        val hungry = if (character.hungry) { "Oui" } else { "Non" }
                        binding.textViewCharacterHungry.text = getString(R.string.character_hungry, hungry)
                        val thirsty = if (character.hungry) { "Oui" } else { "Non" }
                        binding.textViewCharacterThirsty.text = getString(R.string.character_thirsty, thirsty)
                        binding.textViewCharacterAp.text = getString(R.string.character_ap, character.action_points.toString())
                    } else {
                        binding.textviewFirst.text = getString(R.string.last_refresh, since)
                        binding.textViewCharacterName.text = getString(R.string.character_name, "${character.name} est MORT !")
                        binding.textViewCharacterHungry.text = ""
                        binding.textViewCharacterThirsty.text = ""
                        binding.textViewCharacterAp.text = ""
                    }

                }

            } else {
                println("No character data, don't update")
                runOnUiThread {
                    binding.textviewFirst.text = getString(R.string.need_configure)
                }
            }
        })

        systemDataViewModel.systemData.observe(viewLifecycleOwner, { systemData ->
            if (systemData != null) {
                if (systemData.current_grab_error != null) {
                    runOnUiThread {
                        binding.textviewFirst.text = systemData.current_grab_error
                    }
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