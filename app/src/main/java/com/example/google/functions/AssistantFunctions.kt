package com.example.google.functions
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.Cursor
import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import android.media.Ringtone
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StrictMode
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.google.assistant.AssistantViewModel
import com.example.google.utils.UiUtils.logKeeper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ml.quaterion.text2summary.Text2Summary
import com.theartofdev.edmodo.cropper.CropImage
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern


@Suppress("DEPRECATION", "CAST_NEVER_SUCCEEDS")
class AssistantFunctions {
    companion object {
        var dips = 44
        var animationTime = 100
        var REQUEST_CALL = 1
        private var SENDSMS = 2
        var READSMS = 3
        var SHAREAFILE = 4
        var SHAREATEXTFILE = 5
        var READCONTACTS = 6
        var CAPTUREPHOTO = 7
        private var imageIndex: Int = 0
        var REQUEST_CODE_SELECT_DOC: Int = 100
        var REQUEST_ENABLE_BT = 1000
        private var questions: List<String> = listOf(" What is your name? ",
            "What do you love ?",
            "Do You love animals?",
            "Are you free today?",
            "Tell me your hobbies?",
            " What will finally break the internet? ",
            " What's the most useless talent you have?"

        )

        private val imageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/assistant/"
        private var bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        fun speak(message: String, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "")
            assistantViewModel.sendMessageToDatabase(keeper, message)
        }
        fun getDate(textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            val calendar = Calendar.getInstance()
            val formattedDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.time)
            val splitDate = formattedDate.split(",").toTypedArray()
            val date = splitDate[1].trim { it <= ' ' }
            speak("The date is $date", textToSpeech, assistantViewModel, keeper)
        }

        @SuppressLint("SimpleDateFormat")
        fun getTime(textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            val calendar = Calendar.getInstance()
            val format = SimpleDateFormat("HH:mm:ss")
            val time = format.format(calendar.time)
            speak("The Time is $time", textToSpeech, assistantViewModel, keeper)
        }
        fun sendWhatsAppMessage(activity: Activity, context: Context, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            val phoneNumber = extractPhoneNumber(keeper)
            val message = extractMessage(keeper)

            if (phoneNumber != null && phoneNumber.isNotEmpty()) {
                // Format the phone number for WhatsApp
                val whatsappNumber = "+$phoneNumber"

                // Create the intent to open WhatsApp with the provided phone number and message
                val uri = Uri.parse("https://wa.me/$whatsappNumber/?text=${Uri.encode(message)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)

                // Verify that WhatsApp is installed on the device
                intent.`package` = "com.whatsapp"

                try {
                    // Start the WhatsApp activity with the intent
                    activity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Handle if WhatsApp is not installed
                    speak("WhatsApp is not installed on your device.", textToSpeech, assistantViewModel, keeper)
                }
            } else {
                speak("Please provide a valid phone number.", textToSpeech, assistantViewModel, keeper)
            }
        }

        private fun extractPhoneNumber(keeper: String): String? {
            val matcher = Pattern.compile("\\d+").matcher(keeper)
            var phoneNumber = ""
            while (matcher.find()) {
                phoneNumber += matcher.group()
            }
            return phoneNumber
        }

        private fun extractMessage(keeper: String): String {
            // Extract the message after "message" or similar keywords
            val messageIndex = keeper.indexOf("message")
            return if (messageIndex != -1 && messageIndex + 7 < keeper.length) {
                keeper.substring(messageIndex + 7).trim()
            } else {
                "This is a test message from my Google Assistant app."
            }
        }
        fun openFacebook(activity: Activity) {
            val intent = activity.packageManager.getLaunchIntentForPackage("com.facebook.katana")
            intent.let { activity.startActivity(it) }

        }

        fun openMaps(activity: Activity) {
            val intent = activity.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
            intent.let { activity.startActivity(it) }

        }

        fun openGoogle(activity: Activity) {
            val intent = activity.packageManager.getLaunchIntentForPackage("com.android.chrome")
            intent.let { activity.startActivity(it) }

        }


        fun openYoutube(activity: Activity) {
            val intent = activity.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            intent.let { activity.startActivity(it) }

        }

        fun openWhatsAPP(activity: Activity) {
            val intent = activity.packageManager.getLaunchIntentForPackage("com.whatsapp")
            intent.let { activity.startActivity(it) }

        }

        fun openMessages(activity: Activity, context: Context) {
            val intent = activity.packageManager.getLaunchIntentForPackage(Telephony.Sms.getDefaultSmsPackage(context))
            intent.let { activity.startActivity(it) }
        }

        fun openGmail(activity: Activity) {
            val intent = activity.packageManager.getLaunchIntentForPackage("com.google.android.gm")
            intent.let { activity.startActivity(it) }
        }


        @SuppressLint("Recycle")
        @RequiresApi(Build.VERSION_CODES.O)
        fun readSMS(activity: Activity, context: Context, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_SMS
                ) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.READ_SMS),
                    READSMS
                )
                Log.d(logKeeper, "reading sms")
            } else {
                val cursor = activity.contentResolver.query(Uri.parse("content://sms"), null,
                    null, null)
                cursor!!.moveToFirst()
                speak("Your last message was" + cursor.getString(12), textToSpeech, assistantViewModel, keeper)

            }

        }

        fun shareAFile(activity: Activity, context: Context) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    SHAREAFILE
                )
            } else {
                val builder = StrictMode.VmPolicy.Builder()
                StrictMode.setVmPolicy(builder.build())
                val myFileIntent = Intent(Intent.ACTION_GET_CONTENT)
                myFileIntent.type = "application/pdf"
                activity.startActivityForResult(myFileIntent, REQUEST_CODE_SELECT_DOC)
            }
        }

        const val REQUEST_IMAGE_CAPTURE = 1

        fun takeSelfie(activity: Activity, context: Context, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, request it from the user
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_IMAGE_CAPTURE
                )
            } else {
                // Permission granted, proceed with taking the selfie
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (cameraIntent.resolveActivity(activity.packageManager) != null) {
                    // Specify that the front camera should be used
                    cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT)
                    activity.startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
                } else {
                    // Handle if no camera app is available
                    speak("No camera app found on your device", textToSpeech, assistantViewModel, keeper)
                }
            }
        }

        fun shareATextMessage(activity: Activity, context: Context, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    SHAREATEXTFILE
                )
            } else {
                try {
                    val builder = StrictMode.VmPolicy.Builder()
                    StrictMode.setVmPolicy(builder.build())
                    val message = keeper.split("that").toTypedArray()[1]
                    val intentShare = Intent(Intent.ACTION_SEND)
                    intentShare.type = "text/plain"
                    intentShare.putExtra(Intent.EXTRA_TEXT, message)
                    activity.startActivity(Intent.createChooser(intentShare, "Sharing Text"))
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    Log.d("error in sms", e.message.toString())
                    speak("Something went wrong", textToSpeech, assistantViewModel, keeper)
                }
            }
        }

        fun sendSMS(activity: Activity, context: Context, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            Log.d(logKeeper, "started sending")
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.SEND_SMS
                ) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.SEND_SMS),
                    SENDSMS
                )
                Log.d(logKeeper, "sending sms")
            } else {
                try {
                    Log.d(logKeeper, "permission are already given")
                    val keeperReplaced = keeper.replace(" ".toRegex(), "")
                    val number = keeperReplaced.split("o")
                        .toTypedArray()[1].split("t").toTypedArray()[0]
                    val message = keeper.split("that").toTypedArray()[1]
                    Log.d("check", "sms : $message , number : $number")
                    val mySmsManager = SmsManager.getDefault()
                    mySmsManager.sendTextMessage(
                        number.trim { it <= ' ' },
                        null,
                        message.trim { it <= ' ' },
                        null,
                        null
                    )
                    speak("Message has been Send and the message send was $message to $number", textToSpeech, assistantViewModel, keeper)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d("error in sms", e.message.toString())
                    speak("Something went wrong", textToSpeech, assistantViewModel, keeper)
                }
            }
        }

        fun makeAPhoneCall(activity: Activity, context: Context, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            val keeperSplit = keeper.replace(" ".toRegex(), "")
                .split("dial")
            val number = keeperSplit[1]
            if (number.trim { it <= ' ' }.isNotEmpty()) {
                if (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.CALL_PHONE) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity,
                        arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL)
                } else {
                    try {
                        val dial = "tel:$number"
                        speak("Calling  $number", textToSpeech, assistantViewModel, keeper)
                        activity.startActivity(Intent(Intent.ACTION_CALL, Uri.parse(dial)))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("error in call", e.message.toString())
                        speak("Something went wrong", textToSpeech, assistantViewModel, keeper)
                    }

                }
            } else {
                speak("Dial Correct Phone Number", textToSpeech, assistantViewModel, keeper)
            }
        }
        fun search(activity: Activity, keeper: String)
        {
            val uri = Uri.parse("https://www.google.com/search?q=$keeper")
            val gSearchIntent = Intent(Intent.ACTION_VIEW, uri)
            activity.startActivity(gSearchIntent)
        }

        @SuppressLint("Range")
        fun callContact(activity: Activity, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            var number= ""
            if (ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.READ_CONTACTS
                ) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS),
                    READCONTACTS
                )
            } else {
                val name = keeper.split("call").toTypedArray()[1].trim {
                    it <= ' '
                }
                Log.d("check", name)
                try {
                    val phones: Cursor = activity.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)!!
                    while (phones.moveToNext()) {
                        var contactName: String = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                        contactName=contactName.lowercase()
                        if (contactName.contains(name.lowercase())) {
                            number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            Log.d("number", number)
                        }
                    }
                    phones.close()

                    if (number.trim {
                            it <= ' '
                        }.isNotEmpty()) {
                        if (ContextCompat.checkSelfPermission(activity,
                                Manifest.permission.CALL_PHONE) != PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(activity,
                                arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL)
                        } else {
                            val dial = "tel:$number"
                            speak("Calling now", textToSpeech, assistantViewModel, keeper)
                            activity.startActivity(Intent(Intent.ACTION_CALL, Uri.parse(dial)))

                        }
                    } else {
                        speak("Wrong Contact Name", textToSpeech, assistantViewModel, keeper)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d("error in call", e.message.toString())
                    speak("Something went wrong", textToSpeech, assistantViewModel, keeper)
                }


            }
        }

        @SuppressLint("MissingPermission")
        fun turnOnBluetooth(activity: Activity, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            if (!bluetoothAdapter.isEnabled) {
                speak("turning On Bluetooth", textToSpeech, assistantViewModel, keeper)
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activity.startActivityForResult(intent, REQUEST_ENABLE_BT)
            } else {
                speak("Bluetooth is  On", textToSpeech, assistantViewModel, keeper)
            }
        }

        fun turnOffBluetooth(
            context: Context,
            textToSpeech: TextToSpeech,
            assistantViewModel: AssistantViewModel,
            keeper: String
        ) {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter == null) {
                // Device does not support Bluetooth
                speak("Bluetooth is not supported on this device", textToSpeech, assistantViewModel, keeper)
                return
            }

            if (!bluetoothAdapter.isEnabled) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PERMISSION_GRANTED
                ) {
                    // TODO: Handle the case where Bluetooth permission is not granted
                    return
                }
                bluetoothAdapter.disable()
                speak("Turning Off Bluetooth", textToSpeech, assistantViewModel, keeper)
            } else {
                speak("Bluetooth is Off", textToSpeech, assistantViewModel, keeper)
            }
        }


        fun getAllPairedDevices(
            context: Context,
            textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            if (bluetoothAdapter.isEnabled) {
                speak("Paired  Devices are ", textToSpeech, assistantViewModel, keeper)
                var text = ""
                var count = 1
                val devices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
                for (device in devices) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return
                    }
                    text += "\nDevices : $count ${device.name}"
                    count += 1
                }
                speak(text, textToSpeech, assistantViewModel, keeper)
            } else {
                speak("Turn on Bluetooth to get paired devices", textToSpeech, assistantViewModel, keeper)
            }


        }

        fun turnOnFlash(cameraManager: CameraManager, cameraID: String, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraID, true)
                    speak("Flash Turned On", textToSpeech, assistantViewModel, keeper)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                speak("Sorry ! Error Occurred", textToSpeech, assistantViewModel, keeper)
            }
        }

        fun turnOffFlash(cameraManager: CameraManager, cameraID: String, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraID, false)
                    speak("Flash Turned Off", textToSpeech, assistantViewModel, keeper)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun clipBoardCopy(clipboardManager: ClipboardManager, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            val data = keeper.split("that").toTypedArray()[0].trim {
                it <= ' '
            }
            if (data.isNotEmpty()) {
                val clipData = ClipData.newPlainText("text", data)
                clipboardManager.setPrimaryClip(clipData)
                speak("data copied to clipboard that is $data", textToSpeech, assistantViewModel, keeper)

            }
        }

        fun clipBoardSpeak(clipboardManager: ClipboardManager, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            val item = clipboardManager.primaryClip!!.getItemAt(0)
            val pasteData = item.text.toString()
            if (pasteData != "") {
                speak("Data stored in last clipboard is $pasteData", textToSpeech, assistantViewModel, keeper)
            } else {
                speak("Clipboard is Empty", textToSpeech, assistantViewModel, keeper)
            }
        }

        //Capture a photo
        fun capturePhoto(activity: Activity, context: Context, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    CAPTUREPHOTO

                )
            } else {
                val builder = StrictMode.VmPolicy.Builder()
                StrictMode.setVmPolicy(builder.build())
                imageIndex++
                val file = "$imageDirectory$imageIndex.jpg"
                val newFile = File(file)
                try {
                    newFile.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val outputFileUri = Uri.fromFile(newFile)
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
                activity.startActivity(cameraIntent)
                speak("Photo will be saved to $file", textToSpeech, assistantViewModel, keeper)

            }


        }

        fun playRingtone(ringtone: Ringtone, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            speak("Playing", textToSpeech, assistantViewModel, keeper)
            ringtone.play()
        }

        fun stopRingtone(ringtone: Ringtone, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            speak("Stopped", textToSpeech, assistantViewModel, keeper)
            ringtone.stop()
        }

        fun readMe(activity: Activity) {
            CropImage.startPickImageActivity(activity)
        }


        fun getTextFromBitmap(activity: Activity, bitmap: Bitmap, textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            val options = TextRecognizerOptions.Builder()
                // Add any options you need, e.g., setLanguage, etc.
                .build()

            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(options)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val resultText = visionText.text
                    if (keeper.contains("summarise")) {
                        speak("Reading Image and Summarising it :\n" +
                                summariseText(resultText), textToSpeech, assistantViewModel, keeper)
                    } else {
                        speak("Reading Image $resultText", textToSpeech, assistantViewModel, keeper)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(activity.applicationContext, "Error ${e.message}", Toast.LENGTH_SHORT).show()

                }

        }

        private fun summariseText(keeper: String): String {
            val summary: kotlin.jvm.internal.Ref.ObjectRef<*> = kotlin.jvm.internal.Ref.ObjectRef<Any?>()
            summary.element = Text2Summary.Companion.summarize(keeper, 0.04f) as Nothing?
            return summary.element as String
        }



        fun motivationalThoughts(textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {

            val horoscopes: List<String> = listOf("If you want to achieve greatness stop asking for permission.",
                "Things work out best for those who make the best of how things work out.",
                "To live a creative life, we must lose our fear of being wrong.",
                "If you are not willing to risk the usual you will have to settle for the ordinary.",
                "Trust because you are willing to accept the risk, not because it's safe or certain.",
                "Take up one idea. Make that one idea your life--think of it, dream of it, live on that idea. Let the brain, muscles, " +
                        "nerves, every part of your body, be full of that idea, and just leave" +
                        " every other idea alone. This is the way to success.",
                "All our dreams can come true if we have the courage to pursue them.",
                "Good things come to people who wait, but better things come to those who go out and get them.",
                "If you do what you always did, you will get what you always got.",
                "Success is walking from failure to failure with no loss of enthusiasm.",
                "Just when the caterpillar thought the world was ending, he turned into a butterfly.")
            val indexes: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            val index = indexes.random()
            speak(horoscopes[index], textToSpeech, assistantViewModel, keeper)
        }



        fun joke(textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            val jokes = listOf("That bizarre moment when you pick up your car from the garage and you realize that the breaks are still not working, but they made your horn louder.\n",
                " I had a dream where an evil queen forced me to eat a gigantic marshmallow. When I woke up, my pillow was gone.\n",
                "Yesterday I learnt that 20 piranhas can strip all flesh off a man within 15 minutes. - Unfortunately, I also lost my job at the local swimming pool.\n"
            )
            val indexes: List<Int> = listOf(0, 1, 2)
            val index = indexes.random()
            speak(jokes[index], textToSpeech, assistantViewModel, keeper)
        }

        fun question(textToSpeech: TextToSpeech, assistantViewModel: AssistantViewModel, keeper: String) {
            val indexes: List<Int> = listOf(0, 1, 2, 3, 4, 5)
            val index = indexes.random()
            speak(questions[index], textToSpeech, assistantViewModel, keeper)
        }




    }
}
