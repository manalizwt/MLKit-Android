package com.appmarkets.firebasemlkit.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.NotificationManager
import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.android.gms.tasks.OnFailureListener
import android.util.Log
import androidx.annotation.NonNull

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.*
import android.print.PrintAttributes
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import com.appmarkets.firebasemlkit.R
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfWriter
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.translate_langauge_dialog.*
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.spongycastle.asn1.iana.IANAObjectIdentifiers.directory
import org.spongycastle.asn1.x500.style.RFC4519Style.description
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern


class MainActivity : AppCompatActivity() {
    private val TAG = "DocActivity"

    val sb = StringBuilder()

    internal lateinit var arrLanguageCode: Array<Array<String>>

    var lnDelay: Long = 1000 // 1 seconds after user stops typing
    var lnTextEdit: Long = 0
    var handler = Handler()

    var strDetectLanguageCode: String = ""
    var strTranslateLanguageCode: String = ""

    var intDetectLanguageCode: Int = -1
    var intTranslateLanguageCode: Int = -1
    var langCode: Int = -1
    //Text to speech
    internal var tts: TextToSpeech? = null
    internal var progressBar: ProgressBar? = null
    internal lateinit var arrTextToSpecchLangCodes: ArrayList<String>
    internal lateinit var arrLanguagesWithCode: ArrayList<Array<String>>

    private var bmpImage: Bitmap? = null
    internal val REQUEST_IMAGE_CAPTURE = 1
    private val IMAGE_PICK_CODE = 1001;
    //Permission code
    private val PERMISSION_CODE = 1001;

    private var mData: String? = null
    var strTranslatedData = ""

    private val mNotifyManager: NotificationManager? = null

    var strExtension = ""
    var count: Int? = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this);
        setContentView(com.appmarkets.firebasemlkit.R.layout.activity_main)
        progressBar = findViewById(R.id.progressBar)
        progressBar!!.setMax(10);

        setLangsetAdapterSpinner()

        etTextwatcherDataGet()

        // Onclick Listnear
        onClick()

    }

    private fun onClick() {


        // Text To speech Mix button
        btnMic.setOnClickListener {

            var strClear = ""
            etInputText.text = Editable.Factory.getInstance().newEditable(strClear)
            etOutPut.text = Editable.Factory.getInstance().newEditable(strClear)

            if (langCode != -1) {

                val strTexttoSpeech = arrLanguagesWithCode[langCode][2]
                val voice = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                voice.putExtra(RecognizerIntent.EXTRA_LANGUAGE, strTexttoSpeech)
                voice.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, strTexttoSpeech)
                voice.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now!")
                try {
                    startActivityForResult(voice, 100)
                } catch (anfe: ActivityNotFoundException) {

                    Toast.makeText(this, getString(R.string.strNotSupported), Toast.LENGTH_SHORT).show()

                }

            } else {

                val voice = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                voice.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "")
                voice.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "")
                voice.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now!")
                try {
                    startActivityForResult(voice, 100)
                } catch (anfe: ActivityNotFoundException) {

                    Toast.makeText(this, getString(R.string.strNotSupported), Toast.LENGTH_SHORT).show()

                }
            }
        }

        // Clear edit text
        btnClear.setOnClickListener {

            if (tts != null) {
                tts!!.stop()
//                tts!!.shutdown()
            }

            var strClear = ""

            etInputText.text = Editable.Factory.getInstance().newEditable(strClear)
            etOutPut.text = Editable.Factory.getInstance().newEditable(strClear)

            //progressBar!!.visibility = View.INVISIBLE

        }

        // Speaker (Read edittext data)
        btnListen.setOnClickListener {

            val str = Locale(getFromLang())

            tts!!.setLanguage(Locale(getFromLang()))
            if (Build.VERSION.SDK_INT >= 21)
                tts!!.speak(etInputText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            else
                tts!!.speak(etInputText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null)
        }

        //Translate other language
        tvTranslateLangugae.setOnClickListener {

            if(etInputText.text.toString().trim().isNullOrEmpty()){

                Toast.makeText(this, getString(R.string.strEnterTranslaltion), Toast.LENGTH_SHORT).show()
            } else {
                showTranslateLanguageDialogs()
            }
        }

        //Language Translations
        btnTranslate.setOnClickListener {

            if (bmpImage == null) {


                if (intTranslateLanguageCode == -1) {

                    Toast.makeText(this, getString(R.string.strSelectTranslateLan), Toast.LENGTH_SHORT).show()

                } else {
                    count =1;

                    progressBar!!.visibility = View.VISIBLE
                    progressBar!!.setProgress(0);

                    textTranslated().execute(
                        intDetectLanguageCode.toString(),
                        intTranslateLanguageCode.toString(),
                        etInputText.text.toString().trim()
                    )


                    // translatingText(intDetectLanguageCode, intTranslateLanguageCode, etInputText.text.toString().trim())

                }

            } else {

                // detectImageFromText()
            }


        }


        //File Picker

        btnFilePicker.setOnClickListener {
            MaterialFilePicker()
                .withActivity(this@MainActivity)
                .withRequestCode(1000)
                .withFilter(Pattern.compile(".*\\.(pdf|doc)$")) // Filtering files and directories by file name using regexp
                // .withFilterDirectories(true) // Set directories filterable (false by default)
                .withHiddenFiles(true) // Show hidden files and folders
                .start()

        }
    }




    private fun getFromLang(): String {

        return arrLanguagesWithCode.get(langCode)[2];
    }

    // Request focus User typeing and detect language
    private fun etTextwatcherDataGet() {
        etInputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence, start: Int, count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
                //You need to remove this to run only once
                handler.removeCallbacks(input_finish_checker)

            }

            override fun afterTextChanged(s: Editable) {
                //avoid triggering event when text is empty
                if (s.length > 0) {

                    lnTextEdit = System.currentTimeMillis()
                    handler.postDelayed(input_finish_checker, lnDelay)
                } else {

                }

                if (s.length == 0) {

                    tvDetectLangugae.text = getString(
                        R.string.strDetectLangauge
                    )
                    tvTranslateLangugae.text = getString(
                        R.string.strTranslateLangauge
                    )
                    btnListen.visibility = View.GONE
                    btnClear.visibility = View.GONE
                } else {

                    btnListen.visibility = View.VISIBLE
                    btnClear.visibility = View.VISIBLE
                }

            }
        })
    }

    //Identify Language's
    private fun identifyLanguage(strLanguage: String) {

        val languageIdentifier = FirebaseNaturalLanguage.getInstance().languageIdentification
        languageIdentifier.identifyLanguage(strLanguage)
            .addOnSuccessListener { languageCode ->
                if (languageCode !== "und") {
                    detectLanguageFirebaseCode(languageCode)
                } else {
                    Log.e("Not found", "Can't identify language.")
                }
            }
            .addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(@NonNull e: Exception) {

                        Log.e("Error", e.toString())
                    }
                })
    }

    // Detect Language's
    private fun detectLanguageFirebaseCode(language: String) {

        langCode =
            arrTextToSpecchLangCodes.indexOf(language)



        when (language) {

            "en" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.EN
                tvDetectLangugae.setText("English")

                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ar" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.AR

                tvDetectLangugae.setText("Arabic")

                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ur" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.UR

                tvDetectLangugae.setText("Urdu")

                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "af" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.AF
                tvDetectLangugae.setText("Afrikaans")

                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "be" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.BE

                tvDetectLangugae.setText("Belarusian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "bg" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.BG

                tvDetectLangugae.setText("Bulgarian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "bn" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.BN
                tvDetectLangugae.setText("Bengali")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ca" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.CA

                tvDetectLangugae.setText("Catalan")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "cs" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.CS

                tvDetectLangugae.setText("Czech")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "cy" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.CY
                tvDetectLangugae.setText("Welsh")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "da" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.DA

                tvDetectLangugae.setText("Danish")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "de" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.DE

                tvDetectLangugae.setText("German")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "el" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.EL


                tvDetectLangugae.setText("Greek")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "eo" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.EO

                tvDetectLangugae.setText("Esperanto")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "fa" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.FA


                tvDetectLangugae.setText("Persian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "fi" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.FI


                tvDetectLangugae.setText("Finnish")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "fr" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.FR


                tvDetectLangugae.setText("French")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ga" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.GA

                tvDetectLangugae.setText("Irish")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "gl" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.GL

                tvDetectLangugae.setText("Galician")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "gu" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.GU


                tvDetectLangugae.setText("Gujarati")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "he" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.HE

                tvDetectLangugae.setText("Hebrew")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "hi" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.HI

                tvDetectLangugae.setText("Hindi")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "hr" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.HR

                tvDetectLangugae.setText("Croatian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ht" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.HT

                tvDetectLangugae.setText("Haitian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "hu" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.HU


                tvDetectLangugae.setText("Hungarian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "id" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.ID

                tvDetectLangugae.setText("Indonesian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "is" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.IS

                tvDetectLangugae.setText("Icelandic")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "it" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.IT

                tvDetectLangugae.setText("Italian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ja" -> {

                intDetectLanguageCode = FirebaseTranslateLanguage.JA

                tvDetectLangugae.setText("Japanese")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ka" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.GL

                tvDetectLangugae.setText("Georgian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "kn" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.KN

                tvDetectLangugae.setText("Kannada")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ko" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.KO

                tvDetectLangugae.setText("Korean")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "lt" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.LT

                tvDetectLangugae.setText("Lithuanian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "lv" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.LV

                tvDetectLangugae.setText("Latvian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "mk" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.MK

                tvDetectLangugae.setText("Macedonian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "mr" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.MR

                tvDetectLangugae.setText("Marathi")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ms" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.MS

                tvDetectLangugae.setText("Malay")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "mt" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.MT


                tvDetectLangugae.setText("Maltese")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "nl" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.NL

                tvDetectLangugae.setText("Dutch")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "no" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.NO

                tvDetectLangugae.setText("Norwegian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "pl" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.PL

                tvDetectLangugae.setText("Polish")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "pt" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.PT


                tvDetectLangugae.setText("Portuguese")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ro" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.RO

                tvDetectLangugae.setText("Romanian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ru" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.RU
                tvDetectLangugae.setText("Russian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "sk" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.SK
                tvDetectLangugae.setText("Slovak")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "sl" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.SL


                tvDetectLangugae.setText("Slovenian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "sq" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.SQ

                tvDetectLangugae.setText("Albanian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "sv" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.SV

                tvDetectLangugae.setText("Swedish")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "sw" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.SW

                tvDetectLangugae.setText("Swahili")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "ta" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.TA

                tvDetectLangugae.setText("Tamil")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "te" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.TE

                tvDetectLangugae.setText("Telugu")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "th" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.TH

                tvDetectLangugae.setText("Thai")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "tl" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.TL

                tvDetectLangugae.setText("Thai")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "tr" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.TR

                tvDetectLangugae.setText("Turkish")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "uk" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.UK


                tvDetectLangugae.setText("Ukrainian")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "vi" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.VI

                tvDetectLangugae.setText("Vietnamese")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            "zh" -> {
                intDetectLanguageCode = FirebaseTranslateLanguage.ZH


                tvDetectLangugae.setText("Chinese")
                strDetectLanguageCode = intDetectLanguageCode.toString()
            }

            else -> intDetectLanguageCode = 0
        }


    }

    private val input_finish_checker = Runnable {
        if (System.currentTimeMillis() > lnTextEdit + lnDelay - 500) {

            identifyLanguage(etInputText.text.toString().trim())

        }
    }

    // OnActivity Result's
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i("Result($requestCode)", resultCode.toString())

        if (requestCode == 100 && resultCode == RESULT_OK) {
            //When we get result display it inside the input text (EditText)
            val results = data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

            etInputText.setText(results!![0])
            super.onActivityResult(requestCode, resultCode, data)

        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            val extras = data!!.getExtras()
            bmpImage = extras!!.get("data") as Bitmap

        } else if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {

            bmpImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data?.data);

            detectImageFromText()

            progressBar!!.visibility = View.VISIBLE

        } else if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            var filePath = data!!.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)

            if (filePath.contains("pdf")) {

                progressBar!!.visibility = View.VISIBLE
                LetsParse(filePath)
                filePath = ""
                strExtension = "pdf"
            } else {

                progressBar!!.visibility = View.VISIBLE
                sb.clear()
                docFileExtarct(filePath)
                filePath = ""



                strExtension = "doc"


            }
        }
    }

    protected fun LetsParse(filePath: String) {
        PDFBoxResourceLoader.init(this)

        ParsePDFTask().execute(filePath)
    }

    private inner class ParsePDFTask : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String): String? {
            val filePath = params[0]
            try {
                val mTextStripper = PDFTextStripper()
                val file = File(filePath)
                val mfile = PDDocument.load(file)

                val info = mfile.documentInformation
                val cat = mfile.documentCatalog
                val metadata = cat.metadata


                mData = mTextStripper.getText(mfile)

                mfile.close()

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return mData
        }


        override fun onPreExecute() {
            etInputText.setText("")

        }


        override fun onPostExecute(mdata: String) {

            etInputText.setText(mdata)

            progressBar!!.visibility = View.INVISIBLE

        }

        override fun onCancelled() {
            progressBar!!.visibility = View.INVISIBLE

        }
    }

    //OnPause
    override fun onPause() {
        super.onPause()
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
    }

    // OnResume
    override fun onResume() {
        super.onResume()

        tts = TextToSpeech(this, TextToSpeech.OnInitListener { i ->
            if (i != TextToSpeech.ERROR) {
                tts!!.setLanguage(Locale("en_IN"))
                Log.i("Initializing TTS...", "TTS initialized!")
            }
        })
    }

    // Translate language dialog Boxs
    private fun showTranslateLanguageDialogs() {

        val dialog = Dialog(this)

        val view = layoutInflater.inflate(R.layout.translate_langauge_dialog, null)

        val listView = view.findViewById(R.id.lvLangaugeList) as ListView

        val arrayAdapter =
            ArrayAdapter.createFromResource(this, R.array.arrLanguageList, android.R.layout.simple_list_item_1)
        listView.adapter = arrayAdapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            Log.e("Translate Langauge!", arrayAdapter.getItem(position)!!.toString())
            tvTranslateLangugae.setText(arrayAdapter.getItem(position)!!.toString())

            selectLanguageCode(arrayAdapter.getItem(position)!!.toString())


            dialog.dismiss()

        }

        dialog.setContentView(view)
        dialog.show()

    }

    // Translate Language's code
    private fun selectLanguageCode(language: String) {


        when (language) {

            "English" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.EN

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }


            "Arabic" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.AR


                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Urdu" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.UR
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Afrikaans" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.AF

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Belarusian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.BE
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Bulgarian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.BG

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Bengali" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.BN
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Catalan" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.CA

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Czech" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.CS

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Welsh" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.CY
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Danish" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.DA

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "German" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.DE

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Greek" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.EL


                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Esperanto" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.EO

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }


            "Persian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.FA


                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Finnish" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.FI


                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "French" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.FR
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Irish" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.GA

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Galician" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.GL

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Gujarati" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.GU


                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Hebrew" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.HE

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Hindi" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.HI

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Croatian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.HR
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Haitian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.HT
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Hungarian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.HU
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Indonesian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.ID

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Icelandic" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.IS

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Italian" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.IT

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }


            "Japanese" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.JA

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Georgian" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.GL

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Kannada" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.KN

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Korean" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.KO
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Lithuanian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.LT
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Latvian" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.LV

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Macedonian" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.MK

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Marathi" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.MR

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Malay" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.MS

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Maltese" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.MT
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Dutch" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.NL

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Norwegian" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.NO

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "Polish" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.PL

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Portuguese" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.PT


                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Romanian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.RO
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }


            "Russian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.RU
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Slovak" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.SK
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Slovenian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.SL
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Albanian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.SQ
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }


            "Swedish" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.SV
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Swahili" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.SW
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }
            "ta" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.TA

                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Telugu" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.TE
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }


            "Thai" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.TL
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Turkish" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.TR
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Ukrainian" -> {

                intTranslateLanguageCode = FirebaseTranslateLanguage.UK
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Vietnamese" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.VI
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            "Chinese" -> {
                intTranslateLanguageCode = FirebaseTranslateLanguage.ZH
                strTranslateLanguageCode = intTranslateLanguageCode.toString()
            }

            else ->

                intTranslateLanguageCode = 0
        }
    }

    // Adapter
    private fun setLangsetAdapterSpinner() {
        arrLanguageCode = arrayOf(
            arrayOf("**", getString(R.string.auto_detect), "", "1"),
            arrayOf("af", getString(R.string.Afrikaans)),
            arrayOf("sq", getString(R.string.Albanian)),
            arrayOf("am", getString(R.string.Amharic)),
            arrayOf("ar", getString(R.string.Arabic), "ar-XA"),
            arrayOf("hy", getString(R.string.Armenian)),
            arrayOf("az", getString(R.string.Azerbaijani)),
            arrayOf("ba", getString(R.string.Bashkir)),
            arrayOf("eu", getString(R.string.Basque)),
            arrayOf("be", getString(R.string.Belarusian)),
            arrayOf("bn", getString(R.string.Bengali), "bn_IN"), //NO VOICE INPUT
            arrayOf("bs", getString(R.string.Bosnian)),
            arrayOf("bg", getString(R.string.Bulgarian)),
            arrayOf("my", getString(R.string.Burmese)),
            arrayOf("ca", getString(R.string.Catalan)),
            arrayOf("ceb", getString(R.string.Cebuano)),
            arrayOf("zh", getString(R.string.Chinese), "zh_CN", "1"),
            arrayOf("hr", getString(R.string.Croatian)),
            arrayOf("cs", getString(R.string.Czech), "cs_CZ", "1"),
            arrayOf("da", getString(R.string.Danish), "da_DK", "1"),
            arrayOf("nl", getString(R.string.Dutch), "nl_NL", "1"),
            arrayOf("en", getString(R.string.English), "en_IN", "1"),
            arrayOf("eo", getString(R.string.Esperanto)),
            arrayOf("et", getString(R.string.Estonian)),
            arrayOf("fi", getString(R.string.Finnish), "fi_FI", "1"),
            arrayOf("fr", getString(R.string.French), "fr_FR", "1"),
            arrayOf("gl", getString(R.string.Galician)),
            arrayOf("ka", getString(R.string.Georgian)),
            arrayOf("de", getString(R.string.German), "de_DE", "1"),
            arrayOf("el", getString(R.string.Greek)),
            arrayOf("gu", getString(R.string.Gujarati)),
            arrayOf("ht", getString(R.string.Haitian)),
            arrayOf("he", getString(R.string.Hebrew)),
            arrayOf("mrj", getString(R.string.Hill_Mari)),
            arrayOf("hi", getString(R.string.Hindi), "hi_IN", "1"),
            arrayOf("hu", getString(R.string.Hungarian), "hu_HU", "1"),
            arrayOf("is", getString(R.string.Icelandic)),
            arrayOf("id", getString(R.string.Indonesian), "in_ID", "1"),
            arrayOf("ga", getString(R.string.Irish)),
            arrayOf("it", getString(R.string.Italian), "it_IT", "1"),
            arrayOf("ja", getString(R.string.Japanese), "ja_JP", "1"),
            arrayOf("jv", getString(R.string.Javanese)),
            arrayOf("kn", getString(R.string.Kannada)),
            arrayOf("kk", getString(R.string.Kazakh)),
            arrayOf("km", getString(R.string.Khmer), "km_KH"), //NO VOICE INPUT
            arrayOf("ko", getString(R.string.Korean), "ko_KR", "1"),
            arrayOf("ky", getString(R.string.Kyrgyz)),
            arrayOf("lo", getString(R.string.Lao)),
            arrayOf("la", getString(R.string.Latin)),
            arrayOf("lv", getString(R.string.Latvian)),
            arrayOf("lt", getString(R.string.Lithuanian)),
            arrayOf("lb", getString(R.string.Luxembourgish)),
            arrayOf("mk", getString(R.string.Macedonian)),
            arrayOf("mg", getString(R.string.Malagasy)),
            arrayOf("ms", getString(R.string.Malay)),
            arrayOf("ml", getString(R.string.Malayalam)),
            arrayOf("mt", getString(R.string.Maltese)),
            arrayOf("mi", getString(R.string.Maori)),
            arrayOf("mr", getString(R.string.Marathi)),
            arrayOf("mhr", getString(R.string.Mari)),
            arrayOf("mn", getString(R.string.Mongolian)),
            arrayOf("ne", getString(R.string.Nepali), "ne_NP"), //NO VOICE INPUT
            arrayOf("no", getString(R.string.Norwegian), "nn_NO"), //NO VOICE INPUT
            arrayOf("pap", getString(R.string.Papiamento)),
            arrayOf("fa", getString(R.string.Persian)),
            arrayOf("pl", getString(R.string.Polish), "pl_PL", "1"),
            arrayOf("pt", getString(R.string.Portuguese), "pt_BR", "1"),
            arrayOf("pa", getString(R.string.Punjabi)),
            arrayOf("ro", getString(R.string.Romanian)),
            arrayOf("ru", getString(R.string.Russian), "ru_RU", "1"),
            arrayOf("gd", getString(R.string.Scottish_Gaelic)),
            arrayOf("sr", getString(R.string.Serbian)),
            arrayOf("si", getString(R.string.Sinhala), "si_LK"), //NO VOICE INPUT
            arrayOf("sk", getString(R.string.Slovak)),
            arrayOf("sl", getString(R.string.Slovenian)),
            arrayOf("es", getString(R.string.Spanish), "es_ES", "1"),
            arrayOf("su", getString(R.string.Sundanese)),
            arrayOf("sw", getString(R.string.Swahili)),
            arrayOf("sv", getString(R.string.Swedish), "sv_SE", "1"),
            arrayOf("tl", getString(R.string.Tagalog)),
            arrayOf("tg", getString(R.string.Tajik)),
            arrayOf("ta", getString(R.string.Tamil)),
            arrayOf("tt", getString(R.string.Tatar)),
            arrayOf("te", getString(R.string.Telugu)),
            arrayOf("th", getString(R.string.Thai), "th_TH", "1"),
            arrayOf("tr", getString(R.string.Turkish), "tr_TR", "1"),
            arrayOf("udm", getString(R.string.Udmurt)),
            arrayOf("uk", getString(R.string.Ukrainian), "uk_UA", "1"),
            arrayOf("ur", getString(R.string.Urdu)),
            arrayOf("uz", getString(R.string.Uzbek)),
            arrayOf("vi", getString(R.string.Vietnamese), "vi_VN", "1"),
            arrayOf("cy", getString(R.string.Welsh)),
            arrayOf("xh", getString(R.string.Xhosa)),
            arrayOf("yi", getString(R.string.Yiddish))
        )

        arrTextToSpecchLangCodes = ArrayList<String>()
        arrLanguagesWithCode = ArrayList()

        for (i in arrLanguageCode.indices) {
            arrTextToSpecchLangCodes.add(i, arrLanguageCode[i][0])
            arrLanguagesWithCode.add(i, arrLanguageCode[i])
        }


        Log.e("Langauge_code", arrTextToSpecchLangCodes.toString())
    }

    // Detect Image to text
    private fun detectImageFromText() {


        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(bmpImage!!)
        val firebaseVisionTextRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
        firebaseVisionTextRecognizer.processImage(firebaseVisionImage).addOnSuccessListener(
        )
        {

            convetImagetoText(it)

        }.addOnFailureListener {


        }
    }

    // Use Cloud Functions Detect Image to text
    private fun detectCloudImageFromText() {


        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(bmpImage!!)
        val firebaseVisionTextRecognizer = FirebaseVision.getInstance().cloudTextRecognizer
        firebaseVisionTextRecognizer.processImage(firebaseVisionImage).addOnSuccessListener(
        )
        {
            //  convetImagetoText(it)
            val convertText = it.getText()
            Log.e("txt", convertText)

            etInputText.text = Editable.Factory.getInstance().newEditable(convertText)

        }.addOnFailureListener {
            //show error
            Log.e("@@txt", "Error")

        }
    }

    private fun convetImagetoText(text: FirebaseVisionText) {
        val blocks = text.textBlocks
        if (blocks.size == 0) {
            progressBar!!.visibility = View.INVISIBLE

            Toast.makeText(this@MainActivity, "No Text", Toast.LENGTH_LONG).show()
            return
        }

        for (block in text.textBlocks) {
            val convertText = block.getText()
            Log.e("txt", convertText)

            progressBar!!.visibility = View.INVISIBLE

            try {

                bmpImage!!.recycle()
                bmpImage = null

            } catch (e : Exception){


            }
        }
    }

    //handle requested permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Pick Image from gallary
    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)

    }


    private fun translatingText(
        intDetectLanguageCode: Int,
        intTranslateLanguageCode: Int,
        strinputLanguage: String
    ) {

        val options = FirebaseTranslatorOptions.Builder()
            //from language
            .setSourceLanguage(intDetectLanguageCode)
            // to language
            .setTargetLanguage(intTranslateLanguageCode)
            .build()

        val translator = FirebaseNaturalLanguage.getInstance()
            .getTranslator(options)

        val conditions = FirebaseModelDownloadConditions.Builder()
            .build()

        translator.downloadModelIfNeeded(conditions).addOnSuccessListener(OnSuccessListener<Void> {
            translator.translate(strinputLanguage)
                .addOnSuccessListener(
                    OnSuccessListener<String> { translatedText ->
                        //progressBar!!.visibility = View.INVISIBLE

                        val strTranslate = translatedText
                        etOutPut.text = Editable.Factory.getInstance().newEditable(strTranslate)




                    })
                .addOnFailureListener(
                    OnFailureListener { e ->
                        //                                        hideDialog();
                    })
        })


    }


    @SuppressLint("StaticFieldLeak")
    private inner class textTranslated : AsyncTask<String, String, String>() {

        override fun doInBackground(vararg params: String): String? {


            val strDetectLanguageCode = params[0]
            val strTargetLanguageCode = params[1]
            val strTranslateLanguage = params[2]
            try {

                val options = FirebaseTranslatorOptions.Builder()
                    //from language
                    .setSourceLanguage(strDetectLanguageCode.toInt())
                    // to language
                    .setTargetLanguage(strTargetLanguageCode.toInt())
                    .build()

                val translator = FirebaseNaturalLanguage.getInstance()
                    .getTranslator(options)

                val conditions = FirebaseModelDownloadConditions.Builder()
                    .build()


                translator.downloadModelIfNeeded(conditions).addOnSuccessListener(OnSuccessListener<Void> {
                    translator.translate(strTranslateLanguage)

                        .addOnSuccessListener(
                            OnSuccessListener<String> { translatedText ->

                                strTranslatedData = translatedText
                                etOutPut.setText(strTranslatedData)


                                progressBar!!.visibility = View.INVISIBLE

                            })
                        .addOnFailureListener(
                            OnFailureListener { e ->

                            })




                })

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return strTranslatedData
        }


        override fun onPreExecute() {

            progressBar!!.visibility = View.VISIBLE

        }


        override fun onPostExecute(mdata: String) {


            progressBar!!.visibility = View.VISIBLE

        }


        override fun onCancelled() {
            //progressBar!!.visibility = View.INVISIBLE
            progressBar!!.visibility = View.INVISIBLE


        }

    }


    fun docFileExtarct(filePath: String?) {
//        progressBar!!.visibility = View.INVISIBLE
        val `is` = FileInputStream(filePath)
        val extractor = WordExtractor(`is`)


//        val filePaths : File = File(filePath)
//        val oleTextExtractor = ExtractorFactory.createExtractor(`is`)
        var string = ""
        val paraTexts = extractor.getParagraphText()


        for (i in paraTexts.indices) {
            string = paraTexts[i]
            sb.append(string)
            log(string)
        }

        etInputText.text = Editable.Factory.getInstance().newEditable(sb.toString())
        progressBar!!.visibility = View.INVISIBLE

        this.closeStream(`is`)

    }

    fun log(text: String) {
        Log.d(TAG, text)
    }

    private fun closeStream(`is`: InputStream?) {

        if (`is` != null) {
            try {
//                progressBar!!.visibility = View.INVISIBLE

                `is`.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }


    //Create Pdf form translated Text
    fun createandDisplayPdf(text: String) {

        val doc = Document()

        try {
            val path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Dir"

            val dir = File(path)
            if (!dir.exists())
                dir.mkdirs()

            val file = File(dir, "newFile1.pdf")
            val fOut = FileOutputStream(file)

            PdfWriter.getInstance(doc, fOut)

            //open the document
            doc.open()

            val p1 = Paragraph(text)
           // val paraFont = Font(Font.BOLD)
            p1.setAlignment(Paragraph.ALIGN_CENTER)
          //  p1.setFont(paraFont)

            //add paragraph to document
            doc.add(p1)

        } catch (de: DocumentException) {
            Log.e("PDFCreator", "DocumentException:$de")
        } catch (e: IOException) {
            Log.e("PDFCreator", "ioException:$e")
        } finally {
            doc.close()
        }

        viewPdf("newFile1.pdf", "Dir")
    }


    // Method for opening a pdf file
    private fun viewPdf(file: String, directory: String) {

        val pdfFile = File(externalCacheDir!!.absolutePath, "/" + directory + "/" + file)

//        val pdfFile = File(Environment.getExternalStorageDirectory() + "/" + directory + "/" + file)
//        val path = Uri.fromFile(pdfFile)

        val path = FileProvider.getUriForFile(
            this,
            getApplicationContext()
                .getPackageName() + ".provider", pdfFile)


        // Setting the intent for pdf reader
        val pdfIntent = Intent(Intent.ACTION_VIEW)
        pdfIntent.setDataAndType(path, "application/pdf")
        pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        try {
            startActivity(pdfIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this@MainActivity, "Can't read pdf file", Toast.LENGTH_SHORT).show()
        }

    }

    fun createPdf(outputPath: String): String {

        val document = Document()

        val path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Dir"

        val dir = File(path)
        if (!dir.exists())
            dir.mkdirs()

        val file = File(dir, "Translated.pdf")
        val fOut = FileOutputStream(file)

        PdfWriter.getInstance(document, fOut)
        document.open()

        val p1 = Paragraph(outputPath)
        p1.setAlignment(Paragraph.ALIGN_CENTER)

        document.add(p1)
        document.close()

        return path
    }

}
