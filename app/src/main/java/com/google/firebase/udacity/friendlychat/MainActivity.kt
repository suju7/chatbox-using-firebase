package com.google.firebase.udacity.friendlychat

import android.content.Intent
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class MainActivity : AppCompatActivity() {

    private var mMessageListView: ListView? = null
    private var mMessageAdapter: MessageAdapter? = null
    private var mProgressBar: ProgressBar? = null
    private var mPhotoPickerButton: ImageButton? = null
    private var mMessageEditText: EditText? = null
    private var mSendButton: Button? = null
    private var mUsername: String? = null
    private var friendlyMessages = ArrayList<FriendlyMessage>()


    private lateinit var mFireBaseDatabase: FirebaseDatabase
    private lateinit var mMessagesDatabaseReference : DatabaseReference
    private var mChildEventListener: ChildEventListener? = null

    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mAuthStateListener: FirebaseAuth.AuthStateListener
    private lateinit var mFirebaseStorage: FirebaseStorage
    private lateinit var mChatPhotoStorageReference: StorageReference

    companion object {
        private const val TAG = "MainActivity"
        const val ANONYMOUS = "anonymous"
        const val DEFAULT_MSG_LENGTH_LIMIT = 1000
        const val RC_SIGN_IN = 1
        const val RC_PHOTO_PICKER = 2
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mUsername = ANONYMOUS

        mFireBaseDatabase = FirebaseDatabase.getInstance()
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseStorage = FirebaseStorage.getInstance()

        mMessagesDatabaseReference = mFireBaseDatabase.reference.child("messages")
        mChatPhotoStorageReference = mFirebaseStorage.reference.child("chat_photos")

        // Initialize references to views
        mProgressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        mMessageListView = findViewById<View>(R.id.messageListView) as ListView
        mPhotoPickerButton = findViewById<View>(R.id.photoPickerButton) as ImageButton
        mMessageEditText = findViewById<View>(R.id.messageEditText) as EditText
        mSendButton = findViewById<View>(R.id.sendButton) as Button
        mSendButton!!.visibility = View.GONE

        // Initialize message ListView and its adapter
        mMessageAdapter = MessageAdapter(this, R.layout.item_message, friendlyMessages)
        mMessageListView!!.adapter = mMessageAdapter

        mMessageAdapter!!.registerDataSetObserver(object: DataSetObserver(){
            override fun onChanged() {
                super.onChanged()
                mMessageListView!!.smoothScrollToPosition(mMessageAdapter!!.count - 1)
            }
        })
        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton!!.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(
                Intent.createChooser(intent, "Complete action using"),
                RC_PHOTO_PICKER
            )
        }

        // Enable Send button when there's text to send
        mMessageEditText!!.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if(charSequence.toString().trim().isNotEmpty()){
                            mSendButton!!.isEnabled = true
                            mSendButton!!.visibility = View.VISIBLE
                } else {
                    mSendButton!!.isEnabled = false
                    mSendButton!!.visibility = View.GONE
                }
            }
            override fun afterTextChanged(editable: Editable) {}

        })
        mMessageEditText!!.filters = arrayOf<InputFilter>(LengthFilter(DEFAULT_MSG_LENGTH_LIMIT))

        // Send button sends a message and clears the EditText
        mSendButton!!.setOnClickListener { //  Send messages on click
            val friendlyMessage = FriendlyMessage(mMessageEditText!!.text.toString(), mUsername, null)
            mMessagesDatabaseReference.push().setValue(friendlyMessage)
            // Clear input box
            mMessageEditText!!.setText("")
        }



        mAuthStateListener = FirebaseAuth.AuthStateListener {
            val user: FirebaseUser? = mFirebaseAuth.currentUser
            if(user != null){
                // user is signed in
                onSignedInitialize(user.displayName)
            } else{
                // user is signed out
                onSignedOutCleanUp()
                // Choosing authentication providers
                val providers = arrayListOf(
                        AuthUI.IdpConfig.EmailBuilder().build(),
                        AuthUI.IdpConfig.GoogleBuilder().build()
                )

                // Create and launch sign-in intent
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN
                )

            }
        }
        // NOW, ADDING THIS (notorious) mAuthStateListener
        mFirebaseAuth.addAuthStateListener(mAuthStateListener)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){
            if(resultCode == RESULT_OK)
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show()
            else if(resultCode == RESULT_CANCELED) {
                //Toast.makeText(this, "<signed in cancelled>", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        else if(requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){
            val selectedImageUri : Uri? = data?.data
            if(selectedImageUri != null) {
                // get a reference to store file at chat_photos/<FILENAME>
                val photoRef = mChatPhotoStorageReference.child(selectedImageUri.toString())
                val uploadTask = photoRef.putFile(selectedImageUri)
                // upload file to firebase storage
                uploadTask.addOnSuccessListener {
                    val downloadUrl: Task<Uri> = it.storage.downloadUrl
                    downloadUrl.addOnSuccessListener { it2: Uri ->
                        //Log.e(TAG, "******************* Downloaded uri string is : ************** $it2")
                        val friendlyMessage = FriendlyMessage(null, mUsername, it2.toString())
                        mMessagesDatabaseReference.push().setValue(friendlyMessage)
                        Toast.makeText(
                            this@MainActivity,
                            "File uploaded successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.addOnFailureListener{
                    Toast.makeText(this, "Could not upload the file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /*
    override fun onStart() {
        super.onStart()
        // NOW, CALLING THIS (notorious) mAuthStateListener
        //mFirebaseAuth.addAuthStateListener(mAuthStateListener)
    }

    override fun onStop() {
        super.onStop()
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener)
        *//*
        mMessageAdapter?.clear()
        // now detaching the read listener method
        if(mChildEventListener != null)
            mMessagesDatabaseReference.removeEventListener(mChildEventListener!!)
        *//*
    }
*/

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.sign_out_menu -> // sign out
                AuthUI.getInstance().signOut(this)
        }

        return super.onOptionsItemSelected(item)

    }

    fun onSignedInitialize(username: String?){
        mUsername = username
        // now attaching database read listener
        if(mChildEventListener == null) {

            mChildEventListener = object : ChildEventListener {

                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val friendlyMessage = snapshot.getValue(FriendlyMessage::class.java)
                    /*if(friendlyMessage!=null) {
                        val name = friendlyMessage.name
                        val textMsg = friendlyMessage.text
                        val photoUrl = friendlyMessage.photoUrl
                        Log.e(TAG, "NAME ADDED : $name")
                        Log.e(TAG, "TEXT ADDED : $textMsg")
                        Log.e(TAG, "PHOTO URL ADDED : $photoUrl")
                    }*/
                    mMessageAdapter?.add(friendlyMessage)
                    mMessageListView!!.setSelection(mMessageAdapter!!.count-1)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}

            }
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener!!)
        }
        mProgressBar!!.visibility = ProgressBar.INVISIBLE
    }

    fun onSignedOutCleanUp(){
        mUsername = ANONYMOUS
        mMessageAdapter?.clear()
        // now detaching the read listener method
        if(mChildEventListener != null)
            mMessagesDatabaseReference.removeEventListener(mChildEventListener!!)
    }

}