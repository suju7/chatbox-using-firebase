package com.google.firebase.udacity.friendlychat

import android.app.Activity
import android.content.Context
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.firebase.ui.storage.images.FirebaseImageLoader
import java.io.File

class MessageAdapter(context: Context, resource: Int, list: ArrayList<FriendlyMessage>?) :
    ArrayAdapter<FriendlyMessage>(context, resource, list!!) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        lateinit var listItemView: View
        if (convertView == null) {
            listItemView =
                LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)
        }
        else
            listItemView = convertView

        val photoImageView = listItemView.findViewById<ImageView>(R.id.photoImageView)
        val messageTextView = listItemView.findViewById<TextView>(R.id.messageTextView)
        val authorTextView = listItemView.findViewById<TextView>(R.id.nameTextView)

        val current = getItem(position)

        val photoUrl = current!!.photoUrl
        if (photoUrl != null) {
            messageTextView.visibility = View.GONE
            photoImageView.visibility = View.VISIBLE
            Glide.with(context).load(photoUrl).into(photoImageView)
        } else {
            messageTextView.visibility = View.VISIBLE
            photoImageView.visibility = View.GONE
            messageTextView.text = current.text
        }
        authorTextView.text = current.name

        return listItemView
    }
}