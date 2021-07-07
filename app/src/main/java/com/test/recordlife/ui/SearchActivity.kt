package com.test.recordlife.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.test.recordlife.databinding.ActivitySearchBinding
import com.test.recordlife.ui.adapter.NotesAdapter
import com.test.recordlife.ui.adapter.SimpleDividerDecoration

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding

    private lateinit var databaseReference: DatabaseReference

    var allList = ArrayList<HashMap<String, Any>>()
    var searchList = ArrayList<HashMap<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {

        actionBar?.setDisplayHomeAsUpEnabled(true);

        super.onCreate(savedInstanceState)

        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.listSearch.layoutManager = LinearLayoutManager(this)

        binding.listSearch.addItemDecoration(SimpleDividerDecoration(this))

        binding.listSearch.adapter = NotesAdapter(searchList)

        databaseReference =
            Firebase.database("https://recordlife-ad18b-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

        basicListen()

        binding.inputSearch.setOnEditorActionListener { v, actionId, event ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                if (!TextUtils.isEmpty(binding.inputSearch.text.toString())) {

                    search(binding.inputSearch.text.toString())
                } else {

                    searchList.clear()
                    binding.listSearch.adapter?.notifyDataSetChanged()
                }

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (!TextUtils.isEmpty(s.toString())) {
                    search(s.toString())
                } else {

                    searchList.clear()
                    binding.listSearch.adapter?.notifyDataSetChanged()
                }

            }

        })
    }

    private fun search(content: String) {
        searchList.clear()

        allList.filter {
            content.equals(it["userName"].toString()) || it["userName"].toString().contains(content)
                    || content.equals(it["text"].toString()) || it["text"].toString()
                .contains(content)
        }.forEach {
            searchList.add(it)
        }

        binding.listSearch.adapter?.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    private fun basicListen() {

        // [START basic_listen]
        // Get a reference to Messages and attach a listener
        var locationRef = this.databaseReference.child("locations")
        val locationListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // New data at this path. This method will be called after every change in the
                // data at this path or a subpath.

                Log.d("TAG", "Number of messages: ${dataSnapshot.childrenCount}")

                dataSnapshot.children.forEach { child ->
                    // Extract Message object from the DataSnapshot
                    val result: HashMap<String, Any> = child.value as HashMap<String, Any>

                    val latLng: String = result["Latlng"] as String

                    if (result["record"] != null) {

                        val record =
                            result["record"] as HashMap<String, HashMap<String, Any>>

                        allList.clear()

                        allList.addAll(record.values)

                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                // Could not successfully listen for data, log the error
                Log.e("TAG", "messages:onCancelled: ${error.message}")
            }
        }
        locationRef.addValueEventListener(locationListener)
        // [END basic_listen]
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, SearchActivity::class.java)
            context.startActivity(intent)
        }
    }
}