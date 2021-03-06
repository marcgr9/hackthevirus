package ro.htv

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_register.*
import ro.htv.model.Response
import ro.htv.model.User
import ro.htv.utils.AuthRepository
import ro.htv.utils.FirestoreRepository
import ro.htv.utils.StorageRepository
import ro.htv.utils.Utils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate

class Register : AppCompatActivity() {

    private val TAG = "HackTheVirus Register"

    private lateinit var authRepository: AuthRepository
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var storageRepository: StorageRepository

    private val PICK_IMAGE_REQUEST = 123
    private var pictureUri: Uri = Uri.parse(Utils.defaultProfilePicture)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        authRepository = AuthRepository()
        firestoreRepository = FirestoreRepository()
        storageRepository = StorageRepository()

        registerBtn.setOnClickListener {
            validateInputs()
        }

        logo.setOnClickListener {
            selectPicture()
        }
    }

    fun validateInputs() {
        val email = email.text.toString()
        val pass = pass.text.toString()
        val birthday = birthday.text.toString()
        val name = nume.text.toString()

        val err: ArrayList<Int> = ArrayList()

        if (email.isNullOrBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches())
            err.add(R.string.invalidEmail)

        if (pass.isNullOrBlank() || pass.length < 6 || !pass.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")))
            err.add(R.string.invalidPass)

        if (name.isNullOrBlank() || name.length < 3 || !name.matches(Regex("[a-zA-Z ]+")))
            err.add(R.string.invalidName)

        checkDate(birthday)?.let {
            err.add(it)
        }

        var errors = ""
        err.forEach {
            errors += getString(it) + '\n'
        }

        if (errors.isEmpty()) {
            Log.d(TAG, "firebase")

            val user: MutableLiveData<Response> = authRepository.register(email, pass)

            registerBtn.text = "..."
            registerBtn.isClickable = false

            user.observe(this, Observer {
                if (it.ok()) {
                    val userData = User(it.value.toString(), email, name, birthday)
                    val userDocument: MutableLiveData<Response> = firestoreRepository.createUser(userData)

                    userDocument.observe(this, Observer { doc ->
                        if (doc.ok()) {
                            Log.d(TAG, "document creat in firestore pt ${doc.value} cu uid ${it.value}")

                            if (pictureUri.toString() != Utils.defaultProfilePicture) {
                                val source = ImageDecoder.createSource(this.contentResolver, pictureUri)
                                val bitmap = ImageDecoder.decodeBitmap(source)

                                uploadImage(bitmap, it.value.toString())
                            } else {
                                startActivity(Intent(this, TopicSelection::class.java).putExtra("uid", it.value.toString()))
                                finish()
                            }
                        } else {
                            Log.d(TAG, "eroare la creare doc user ${it.value}")
                            errField.text = it.value.toString()
                            registerBtn.text = getString(R.string.register)
                            registerBtn.isClickable = true
                        }
                    })
                } else {
                    errField.text = it.value.toString()
                    registerBtn.text = getString(R.string.register)
                    registerBtn.isClickable = true
                }
            })

        } else {
            errField.text = errors
            registerBtn.text = getString(R.string.register)
            registerBtn.isClickable = true
        }
    }

    private fun selectPicture() {
        Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI
        ).also {
            startActivityForResult(it, PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            pictureUri = data!!.data!!

            Log.d(TAG, pictureUri.toString())

            Glide.with(this)
                    .load(pictureUri)
                    .circleCrop()
                    .into(logo)
        }
    }

    fun checkDate(_date: String?): Int? {
        if (_date.isNullOrBlank()) return R.string.invalidDateFormat
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        dateFormat.isLenient = false
        try {
            dateFormat.parse(_date.trim { it <= ' ' })
        } catch (pe: ParseException) {
            return R.string.invalidDateFormat
        }

        val dateList: List<String> = _date.split("/")

        val date: LocalDate = LocalDate.of(dateList[2].toInt(), dateList[1].toInt(), dateList[0].toInt())
        val today: LocalDate = LocalDate.now()

        if (date.isBefore(today.minusYears(124)) || date.isAfter(today.minusYears(14)))
            return R.string.invalidDate

        return null
    }

    fun uploadImage(bitmap: Bitmap, uid: String) {
        val img: MutableLiveData<Response> = storageRepository.uploadImage(bitmap, uid)

        img.observe(this, Observer {
            if (it.ok()) {
                Log.d(TAG, "imagine uploadata: ${it.value}")

                val linkSetat = firestoreRepository.setImage(uid, it.value.toString())

                linkSetat.observe(this, Observer {link ->
                    if (link) {
                        startActivity(Intent(this, TopicSelection::class.java).putExtra("uid", uid))
                        finish()
                    }
                    else {
                        // ma pis pe el
                    }
                })

            } else {
                errField.text = it.value.toString()
                registerBtn.text = getString(R.string.register)
                registerBtn.isClickable = true
            }
        })
    }
}
