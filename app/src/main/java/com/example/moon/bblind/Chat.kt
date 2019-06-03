package com.example.moon.bblind


import android.content.Intent
import android.util.Log
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*

import android.widget.TextView
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.*
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_chattingroom.*
import org.json.JSONObject

import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

class Chat : AppCompatActivity(), View.OnClickListener {

    private val RC_SIGN_IN = 1001
    private val FCM_MESSAGE_URL = "https://fcm.googleapis.com/fcm/send"
    private val SERVER_KEY = "AAAAghhryJU:APA91bE5FeyIHILMSGcWRgWY4hp43aQv9a5haGPMw2A5ZM0G6-102amS9gh-6YKLRRs4qAAKBE-dCBE7A1fnUjoEi3A6mZgrjVIGz-Y34x_yuOYk4fHSM_wT969p36N5oYgYobr3tyCq"

    // Firebase - Realtime Database
    private var mFirebaseDatabase: FirebaseDatabase? = null
    private var mDatabaseReference: DatabaseReference? = null
    private var mChildEventListener: ChildEventListener? = null

    // Firebase - Authentication
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var mGoogleApiClient: GoogleApiClient? = null

    // Views
    private var mListView: ListView? = null
    private var mEdtMessage: EditText? = null
    private var mBtnGoogleSignIn: SignInButton? = null // 로그인 버튼
    private var mBtnGoogleSignOut: Button? = null // 로그아웃 버튼
    private var mTxtProfileInfo: TextView? = null // 사용자 정보 표시
    private var mImgProfile: ImageView? = null // 사용자 프로필 이미지 표시

    // Values
    private var mAdapter: ChatAdapter? = null
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chattingroom)
        initViews()
        initFirebaseDatabase()
        initFirebaseAuth()
        initValues()
    }

    private fun initViews() {
        mListView = list_message as ListView
        mAdapter = ChatAdapter(this, 0)
        mListView!!.adapter = mAdapter
        mListView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val chatData = mAdapter!!.getItem(position)
            if (!TextUtils.isEmpty(chatData.userEmail)) {
                val editText = EditText(this@Chat)
                AlertDialog.Builder(this@Chat)
                        .setMessage(chatData.userEmail + " 님 에게 메시지 보내기")
                        .setView(editText)
                        .setPositiveButton("보내기") { dialog, which -> sendPostToFCM(chatData, editText.text.toString()) }
                        .setNegativeButton("취소") { dialog, which ->
                            // not thing..
                        }.show()
            }
        }

        mEdtMessage = edit_message as EditText
        btn_send.setOnClickListener(this)

        mBtnGoogleSignIn = btn_google_signin as SignInButton
        mBtnGoogleSignOut = btn_google_signout as Button
        mBtnGoogleSignIn!!.setOnClickListener(this)
        mBtnGoogleSignOut!!.setOnClickListener(this)

        mTxtProfileInfo = txt_profile_info as TextView
        mImgProfile = img_profile as ImageView
    }

    private fun initFirebaseDatabase() {
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mDatabaseReference = mFirebaseDatabase!!.getReference("Chat").child("message")
        mChildEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val chatData = dataSnapshot.getValue(ChatData::class.java)
                chatData!!.firebaseKey = dataSnapshot.key
                mAdapter!!.add(chatData)
                mListView!!.smoothScrollToPosition(mAdapter!!.count)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val firebaseKey = dataSnapshot.key
                val count = mAdapter!!.count
                for (i in 0 until count) {
                    if (mAdapter!!.getItem(i).firebaseKey.equals(firebaseKey)) {
                        mAdapter!!.remove(mAdapter!!.getItem(i))
                        break
                    }
                }
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        mDatabaseReference!!.addChildEventListener(mChildEventListener!!)
    }

    private fun initFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this) { }
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
        mAuthListener = FirebaseAuth.AuthStateListener { updateProfile() }
    }

    private fun initValues() {
        val user = mAuth!!.currentUser
        if (user == null) {
            userName = "Guest" + Random().nextInt(5000)
        } else {
            userName = user.displayName
        }
    }

    private fun updateProfile() {
        val user = mAuth!!.currentUser
        if (user == null) {
            // 비 로그인 상태 (메시지를 전송할 수 없다.)
            mBtnGoogleSignIn!!.visibility = View.VISIBLE
            mBtnGoogleSignOut!!.visibility = View.GONE
            mTxtProfileInfo!!.visibility = View.GONE
            mImgProfile!!.visibility = View.GONE
            btn_send.setVisibility(View.GONE)
            mAdapter!!.setEmail(null)
            mAdapter!!.notifyDataSetChanged()
        } else {
            // 로그인 상태
            mBtnGoogleSignIn!!.visibility = View.GONE
            mBtnGoogleSignOut!!.visibility = View.VISIBLE
            mTxtProfileInfo!!.visibility = View.VISIBLE
            mImgProfile!!.visibility = View.VISIBLE
            btn_send.setVisibility(View.VISIBLE)

            userName = user.displayName // 채팅에 사용 될 닉네임 설정
            val email = user.email
            val profile = StringBuilder()
            profile.append(userName).append("\n").append(user.email)
            mTxtProfileInfo!!.text = profile
            mAdapter!!.setEmail(email)
            mAdapter!!.notifyDataSetChanged()

            Picasso.with(this).load(user.photoUrl).into(mImgProfile)

            val userData = UserData()
            userData.userEmailID = email!!.substring(0, email.indexOf('@'))
            userData.fcmToken = FirebaseInstanceId.getInstance().token

            mFirebaseDatabase!!.getReference("users").child(userData.userEmailID!!).setValue(userData)
        }
    }

    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signOut() {
        mAuth!!.signOut()
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback { updateProfile() }
    }

    private fun sendPostToFCM(chatData: ChatData, message: String) {
        mFirebaseDatabase!!.getReference("users")
                .child(chatData.userEmail!!.substring(0, chatData.userEmail!!.indexOf('@')))
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val userData = dataSnapshot.getValue(UserData::class.java)

                        Log.d("checkkk", dataSnapshot.toString())

                        Thread(object : Runnable {
                            internal var token: String? = null
                            override fun run() {
                                try {

                                    // FMC 메시지 생성 start
                                    val root = JSONObject()
                                    val notification = JSONObject()
                                    notification.put("body", message)
                                    notification.put("title", getString(R.string.app_name))
                                    root.put("notification", notification)
                                    root.put("to", userData!!.fcmToken)   // FMC 메시지 생성 end

                                    val Url = URL(FCM_MESSAGE_URL)
                                    val conn = Url.openConnection() as HttpURLConnection
                                    conn.requestMethod = "POST"
                                    conn.doOutput = true
                                    conn.doInput = true
                                    conn.addRequestProperty("Authorization", "key=$SERVER_KEY")
                                    conn.setRequestProperty("Accept", "application/json")
                                    conn.setRequestProperty("Content-type", "application/json")
                                    val os = conn.outputStream
                                    os.write(root.toString().toByteArray(charset("utf-8")))
                                    os.flush()
                                    conn.responseCode


                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                            }
                        }).start()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
    }

    public override fun onStart() {
        super.onStart()
        mAuth!!.addAuthStateListener(mAuthListener!!)
    }

    public override fun onStop() {
        super.onStop()
        mAuth!!.removeAuthStateListener(mAuthListener!!)
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onDestroy() {
        super.onDestroy()
        mDatabaseReference!!.removeEventListener(mChildEventListener!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                val account = result.signInAccount
                val credential = GoogleAuthProvider.getCredential(account!!.idToken, null)
                mAuth!!.signInWithCredential(credential)
                        .addOnCompleteListener(this) { task ->
                            if (!task.isSuccessful) {
                                Toast.makeText(this@Chat, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show()
                            }
                        }
            } else {
                updateProfile()
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_send -> {
                val message = mEdtMessage!!.text.toString()
                if (!TextUtils.isEmpty(message)) {
                    mEdtMessage!!.setText("")
                    val chatData = ChatData()
                    chatData.userName = userName
                    chatData.message = message
                    chatData.time = System.currentTimeMillis()
                    chatData.userEmail = mAuth!!.currentUser!!.email // 사용자 이메일 주소
                    chatData.userPhotoUrl = mAuth!!.currentUser!!.photoUrl!!.toString() // 사용자 프로필 이미지 주소
                    mDatabaseReference!!.push().setValue(chatData)
                }
            }
            R.id.btn_google_signin -> signIn()
            R.id.btn_google_signout -> signOut()
        }
    }


}
