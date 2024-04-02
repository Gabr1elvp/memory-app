package com.example.memorygame
import memory.utils.MusicService
import android.animation.ArgbEvaluator
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.content.Context
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.IBinder
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.squareup.picasso.Picasso
import memory.utils.EXTRA_BOARD_SIZE
import memory.utils.EXTRA_GAME_NAME
import models.BoardSize
import models.MemoryGame
import models.UserImageList
import java.util.Locale

class MainActivity : AppCompatActivity() {

    //Bind the MusicService

    private var musicService: MusicService? = null
    private var isServiceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            musicService = null
            isServiceBound = false
        }
    }


    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            unbindService(connection)
            isServiceBound = false
        }
    }
    companion object {
       const val TAG = "MainActivity"
    }


    // Layout components

    private lateinit var clRoot: CoordinatorLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var btnNewGame: Button
    private lateinit var btnCustomGame: Button
    private lateinit var btnDownloadGame: Button
    private lateinit var btnOptions: Button
    private lateinit var btnAbout: Button
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private lateinit var adapterSpinner: ArrayAdapter<String>
    private lateinit var createActivityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var audioManager: AudioManager
    private lateinit var rgLayout: RadioGroup
    private lateinit var swSong: Switch
    private lateinit var btnSave: Button
    private lateinit var tvVolume: TextView
    private lateinit var btnExit: Button
    private lateinit var optionsLayout: View
    private lateinit var spinner: Spinner
    private lateinit var sharedPreferences: SharedPreferences


    // String resources

    private val numberMovesEasy = R.string.number_moves_easy
    private val numberMovesMedium = R.string.number_moves_medium
    private val numberMovesHard = R.string.number_moves_hard
    private val numberPairsEasy = R.string.number_pairs_easy
    private val numberPairsMedium = R.string.number_pairs_medium
    private val numberPairsHard = R.string.number_pairs_hard
    private val alertDialogExit = R.string.alert_dialog_exit
    private val toastMainMenu= R.string.toast_main_menu
    private val snackBarFileMissing= R.string.snack_bar_file_missing
    private val snackBarCustomGame = R.string.snack_bar_custom_game
    private val snackInvalidMove = R.string.snack_invalid_move
    private val snackBarWon = R.string.snack_bar_already_won
    private val snackBarWin = R.string.snack_bar_win
    private val creationDialog = R.string.creation_dialog
    private val negativeButton = R.string.negative_button
    private val positiveButton = R.string.positive_button
    private val numberPairs = R.string.number_pairs
    private val numberMoves = R.string.number_moves
    private val chooseDifficulty = R.string.choose_difficulty
    private val downloadCustomGame = R.string.download_custom_game
    private val serverOff = R.string.server_off


    private var language:String?=null
    private var isSongEnabled = false // Flag to indicate the initial selection
    private var volume: Float? = null // Flag to indicate the initial selection
    private var skProgress: Int? = null // Flag to indicate the initial selection
    private var checkedLayout:Int? = null // Flag to indicate the initial selection
    private var isFirstSelection = true // Flag to indicate the initial selection
    private var languages = arrayOf("English", "Português")
    private var hideRetryOption = false
    private var isBoardSetupCompleted = false
    private var drawableResource: Int? = null
    private var isInMainMenu = true
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
    private var boardSize: BoardSize = BoardSize.EASY
    private val db = Firebase.firestore
    
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)

        createActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val dataIntent = result.data
                    val customGameName = dataIntent?.getStringExtra(EXTRA_GAME_NAME)
                    if (customGameName == null) {
                        Log.e(TAG, "Got null custom game from CreateActivity")
                    } else
                    {
                        downloadGame(customGameName)
                    }
                }
            }

        // Retrieve the saved switch state, defaulting to true if not found
        sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
                isSongEnabled = sharedPreferences.getBoolean("isSongEnabled", false)
                language = sharedPreferences.getString("language", language).toString()

        //Check what language its set
                if (language == "Português") {
                    Log.i(TAG, "Português")
                    setLocale("pt")
                } else if (language == "English"){
                    Log.i(TAG, "Inglês")
                    setLocale("en")
                }
                onSelectMenuButtons()


        Log.i(TAG, "onCreate")
    }

    override fun onStart() {

        super.onStart()

        sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        // Retrieve the saved switch state, defaulting to true if not found
        isSongEnabled = sharedPreferences.getBoolean("isSongEnabled", false)
        volume = sharedPreferences.getFloat("volume", 0.5F)

        //Start the song
        if (isSongEnabled && isServiceBound) {
            musicService?.resumePlayback()
            Log.i(TAG, "isSongEnabled yes")
            musicService?.setVolume(volume!!, volume!!)
        }
        else if (isSongEnabled && !isServiceBound) {
            val serviceIntent = Intent(this, MusicService::class.java)
            bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        }

        // Check the saved preference user's layout
        optionsLayout = LayoutInflater.from(this).inflate(R.layout.options, null)
        checkedLayout = sharedPreferences.getInt("checkedLayout", R.id.rbLayout1)
        Log.i(TAG, "checkedLayout '$checkedLayout")
        rgLayout = optionsLayout.findViewById(R.id.rgLayout)
        rgLayout.check(checkedLayout!!)

    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        onSelectMenuButtons()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the playback state of the song
        outState.putBoolean("isSongPlaying", musicService?.isPlaying()!!)
    }

    private fun onSelectMenuButtons() {
        hideRetryOption = false

        setContentView(R.layout.activity_menu)
        supportActionBar?.title = getString(R.string.app_name)

        btnNewGame = findViewById(R.id.btnNewGame)
        btnCustomGame = findViewById(R.id.btnCustomGame)
        btnDownloadGame = findViewById(R.id.btnDownloadGame)
        btnOptions = findViewById(R.id.btnOptions)
        btnAbout = findViewById(R.id.btnAbout)

        // Button listeners

        btnNewGame.setOnClickListener {
            setupMainLayout()
            showNewSizeDialog()
        }

        btnCustomGame.setOnClickListener {
            setupMainLayout()
            showCreationDialog()

        }

        btnDownloadGame.setOnClickListener {
            setupMainLayout()
            showDownloadDialog()
        }

        btnOptions.setOnClickListener {
            hideRetryOption = true
            isInMainMenu = false
0
            invalidateOptionsMenu()
            setContentView(optionsLayout)
            swSong = findViewById(R.id.swSong)
            volumeSeekBar = findViewById(R.id.sbSong)
            rgLayout = findViewById(R.id.rgLayout)
            btnSave = findViewById(R.id.btnSave)
            tvVolume = findViewById(R.id.tvVolume)
            spinner = findViewById(R.id.spinner)

            // Get the user's saved preferences

            sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
            isSongEnabled = sharedPreferences.getBoolean("isSongEnabled", false)
            checkedLayout = sharedPreferences.getInt("checkedLayout", R.id.rbLayout1)
            skProgress = sharedPreferences.getInt("progress", 50)
            volume = sharedPreferences.getFloat("volume", 50F)

            musicService?.setVolume(volume!!, volume!!)

            volumeSeekBar.progress = skProgress!!
            rgLayout.check(checkedLayout!!)
            language = sharedPreferences.getString("language", language).toString()

            // Create an ArrayAdapter using the string array and a default spinner layout
            adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
            // Specify the layout to use when the list of choices appears
            adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

             // Apply the adapter to the spinner
            spinner.adapter = adapterSpinner

            Log.i(TAG, "language $language")
            val position = (spinner.adapter as ArrayAdapter<String>).getPosition(language)
            spinner.setSelection(position)

            swSong.isChecked = isSongEnabled

            volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    // Calculate the volume level based on the progress of the SeekBar
                    volume = progress / 100.0f // Volume ranges from 0.0 to 1.0

                    skProgress = progress

                    // Set the volume of the media player
                    musicService?.setVolume(volume!!, volume!!)

                    sharedPreferences.edit().putInt("progress", skProgress!!).apply()
                    sharedPreferences.edit().putFloat("volume", volume!!).apply()

                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Not used
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Not used
                }
            })

            // Song Listener
            swSong.setOnCheckedChangeListener { _, isChecked ->

                sharedPreferences.edit().putBoolean("isSongEnabled", isChecked).apply()
                if (isChecked) {
                    if (isServiceBound) {
                        musicService?.resumePlayback()
                    }
                    else {
                        val serviceIntent = Intent(this, MusicService::class.java)
                        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
                    }
                    volumeSeekBar.visibility = View.VISIBLE
                    tvVolume.visibility = View.VISIBLE

                } else {
                    volumeSeekBar.visibility = View.GONE
                    tvVolume.visibility = View.GONE
                    musicService?.pause()
                }
            }
            // RadioGroup Listener
            rgLayout.setOnCheckedChangeListener { _, checkedId ->

                when (checkedId) {
                    R.id.rbLayout1 -> {
                        drawableResource = R.drawable.card_layout_1
                    }

                    R.id.rbLayout2 -> {
                        drawableResource = R.drawable.card_layout_2
                    }

                    R.id.rbLayout3 -> {
                        drawableResource = R.drawable.card_layout_3
                    }
                }
                sharedPreferences.edit().putInt("checkedLayout", checkedId).apply()

            }

            // SaveButton Listener
            btnSave.setOnClickListener {
                invalidateOptionsMenu()
                onSelectMenuButtons()
            }



            // Listener to handle the language
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                    if (isFirstSelection) {
                        isFirstSelection = false
                        return  // Skip the initial invocation
                    }

                    val selectedItem = parent?.getItemAtPosition(position).toString()
                    sharedPreferences.edit().putString("language", selectedItem).apply()
                    if (selectedItem == "Português") {

                        Log.i(TAG, "Português")
                        setLocale("pt")
                    } else if (selectedItem == "English"){

                        Log.i(TAG, "Inglês")
                        setLocale("en")
                    }

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }

            }

            }
        btnAbout.setOnClickListener {
            setContentView(R.layout.about_page)
            hideRetryOption = true
            invalidateOptionsMenu()
            btnExit = findViewById(R.id.btnExit)
            btnExit.setOnClickListener {
                onSelectMenuButtons()
            }

        }
    }

    // Apply the user language preference
private fun setLocale(languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    val config = Configuration(resources.configuration)
    config.setLocale(locale)


    // Update application context configuration
    applicationContext.resources.updateConfiguration(config, applicationContext.resources.displayMetrics)

    // Use the new context to update the activity's resources and configuration
    resources.updateConfiguration(config, resources.displayMetrics)

    optionsLayout = LayoutInflater.from(this).inflate(R.layout.options, null)


}
    // Prepare the main layout
    private fun setupMainLayout() {
        setContentView(R.layout.activity_main)
        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)

    }
    // Check if the user is in Main Menu
    private fun isInMainMenu(): Boolean {

        return isInMainMenu

    }
    // Inflate the menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        if (isBoardSetupCompleted) {
            menuInflater.inflate(R.menu.menu_main, menu)
            return true
        }
        else if (hideRetryOption){
            menuInflater.inflate(R.menu.menu_main, menu)
            val refreshItem = menu?.findItem(R.id.mi_Refresh)
            refreshItem?.isVisible = false

            return true
        }
        return false
    }

    // Handle the menu options
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.mi_Refresh -> {
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                    showAlertDialog(getString(alertDialogExit), null, View.OnClickListener {
                        setupBoard()
                    })
                }
                else {
                    setupBoard()
                }
                return true
            }

            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }

            R.id.mi_custom -> {
                showCreationDialog()
            }

            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }
            R.id.mi_Home -> {

                // After the board setup is completed, set the flag to true
                isBoardSetupCompleted = false

                // Invalidate the options menu to trigger onCreateOptionsMenu
                invalidateOptionsMenu()

                if (isInMainMenu()) {
                    Toast.makeText(this, getString(toastMainMenu), Toast.LENGTH_SHORT).show()
                } else {
                    onSelectMenuButtons()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Show the download Dialog
    private fun showDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board, null)
        showAlertDialog(getString(downloadCustomGame), boardDownloadView, View.OnClickListener {
            //Grab the text of the game name that the user wants to download
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)

        })

    }

    // Download a personalized game
// Download a personalized game from Firestore
    private fun downloadGame(customGameName: String) {
        // Retrieve the document corresponding to the custom game name
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            // Convert the Firestore document to a UserImageList object
            val userImageList = document.toObject(UserImageList::class.java)
            // Check if the retrieved userImageList or its images are null
            if (userImageList?.images == null) {
                // If the images are null, display a Snackbar indicating an invalid game and prompt for retry
                val invalidGame = getString(snackBarFileMissing)
                Log.e(TAG, "Invalid custom game data from Firestore")
                // Show the download dialog to allow retry
                showDownloadDialog()
                Snackbar.make(clRoot, "$invalidGame $customGameName", Snackbar.LENGTH_LONG).show()
                // Exit the success listener
                return@addOnSuccessListener
            }
            // Calculate the number of cards based on the number of images fetched
            val numCards = userImageList.images.size * 2
            // Determine the board size based on the number of cards
            boardSize = BoardSize.getByValue(numCards)
            // Retrieve the custom game images from the fetched userImageList
            customGameImages = userImageList.images
            // Pre-fetch the images using Picasso to improve loading speed
            for (imageUrl in userImageList.images) {
                Picasso.get().load(imageUrl).fetch()
            }
            // Display a Snackbar indicating successful custom game download
            val customGame = getString(snackBarCustomGame)
            Snackbar.make(clRoot, "$customGame $customGameName!", Snackbar.LENGTH_LONG).show()
            // Set the game name to the custom game name
            gameName = customGameName

            Log.i(TAG, "boardSize, '$boardSize', customGameImages, '$customGameImages'")
            // Set up the game board with the downloaded custom game
            setupBoard()
        }.addOnFailureListener { exception ->

            Log.e(TAG, "Exception when retrieving game", exception)
            // Display a Snackbar indicating failure to retrieve the game and prompt for retry
            Snackbar.make(clRoot, getString(serverOff), Snackbar.LENGTH_LONG).show()
            // Show the download dialog to allow retry
            showDownloadDialog()
        }
    }


    // Show the Dialog for the user choose the size
    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)

        }

        showAlertDialog(getString(chooseDifficulty), boardSizeView, View.OnClickListener {
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null

            setupBoard()
        })
    }

    // Create Person game dialog
    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)

        showAlertDialog(getString(creationDialog), boardSizeView, View.OnClickListener {

            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }

            // Navigate to a new activity
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            Log.i(TAG, "Intent extra: $desiredBoardSize")
            createActivityResultLauncher.launch(intent)

        })
    }


    // AlertDialog builder
    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton(getString(negativeButton)) {_,_ ->
                onSelectMenuButtons()
            }
            .setPositiveButton(getString(positiveButton)) {_,_ ->
                positiveClickListener.onClick(null)

            }.show()
            .setCancelable(false)

    }

    // Setup board
    private fun setupBoard() {

        // After the board setup is completed, set the flag to true
        isBoardSetupCompleted = true

        // Invalidate the options menu to trigger onCreateOptionsMenu
        invalidateOptionsMenu()

        isInMainMenu = false
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when (boardSize) {
            BoardSize.EASY -> {
                tvNumMoves.text = getString(numberMovesEasy)
                tvNumPairs.text = getString(numberPairsEasy)
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = getString(numberMovesMedium)
                tvNumPairs.text = getString(numberPairsMedium)
            }
            BoardSize.HARD -> {
                tvNumMoves.text = getString(numberMovesHard)
                tvNumPairs.text = getString(numberPairsHard)
            }
        }
        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))

        memoryGame = MemoryGame(boardSize, customGameImages)

        Log.i(TAG, "customGameImages: '$customGameImages', memoryGameCards: '${memoryGame.cards}")

        //Create the Memory Board Adapter
        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, drawableResource, object: MemoryBoardAdapter.CardClickListener {
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }
        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())

    }

    //Update the game when a card is flipped
    private fun updateGameWithFlip(position: Int) {

        if (memoryGame.haveWonGame()) {
            Snackbar.make(clRoot, getString(snackBarWon), Snackbar.LENGTH_LONG).show()
            return
        }

        if (memoryGame.isCardFacedUp(position)) {
            Snackbar.make(clRoot, getString(snackInvalidMove), Snackbar.LENGTH_SHORT).show()
            return
        }

        //Actually flip over the card
        if (memoryGame.flipCard(position)) {
            Log.i(TAG, "Found a match! Num pairs found: ${memoryGame.numPairsFound}")
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full),
            ) as Int

            val numberPairs = getString(numberPairs)
            tvNumPairs.setTextColor(color)
            tvNumPairs.text = "$numberPairs ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if (memoryGame.haveWonGame()){
                Snackbar.make(clRoot, getString(snackBarWin), Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.YELLOW, Color.GREEN, Color.MAGENTA)).oneShot()

            }
        }
        val numberMoves= getString(numberMoves)

        tvNumMoves.text = "$numberMoves ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()

    }

}