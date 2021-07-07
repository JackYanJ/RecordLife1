package com.test.recordlife.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.test.recordlife.Const
import com.test.recordlife.databinding.ActivityListBinding
import com.test.recordlife.ui.adapter.NotesAdapter
import com.test.recordlife.ui.adapter.SimpleDividerDecoration

class ListActivity : BaseActivity() {

    private lateinit var binding: ActivityListBinding

    private lateinit var databaseReference: DatabaseReference

    private lateinit var data: String
    private var type: Int = Const.ListType.LIST_OTHER_USER

    override fun onCreate(savedInstanceState: Bundle?) {
        actionBar?.setDisplayHomeAsUpEnabled(true);

        super.onCreate(savedInstanceState)

        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        data = intent.getStringExtra("data").toString()
        type = intent.getIntExtra("type", Const.ListType.LIST_OTHER_USER)

        binding.list.layoutManager = LinearLayoutManager(this)

        binding.list.addItemDecoration(SimpleDividerDecoration(this))

        databaseReference =
            Firebase.database("https://recordlife-ad18b-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

        basicListen()
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
        if (type == Const.ListType.LIST_OTHER_USER){
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
                        if (latLng.equals(data)) {
                            if (result["record"] != null) {

                                val record =
                                    result["record"] as HashMap<String, HashMap<String, Any>>

                                val arrayList = ArrayList<HashMap<String, Any>>(record.values)

                                binding.list.adapter = NotesAdapter(arrayList)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Could not successfully listen for data, log the error
                    Log.e("TAG", "messages:onCancelled: ${error.message}")
                }
            }
            locationRef.addValueEventListener(locationListener)
        }else{
            var locationRef = this.databaseReference.child("users")
            val locationListener = object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // New data at this path. This method will be called after every change in the
                    // data at this path or a subpath.

                    Log.d("TAG", "Number of messages: ${dataSnapshot.childrenCount}")

                    dataSnapshot.children.filter {
                        it.key.equals(data)
                    }.forEach { child ->
                        // Extract Message object from the DataSnapshot

                        val result: HashMap<String, HashMap<String, Any>> = child.value as HashMap<String, HashMap<String, Any>>

                        val arrayList = ArrayList<HashMap<String, Any>>(result.values)

                        binding.list.adapter = NotesAdapter(arrayList)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Could not successfully listen for data, log the error
                    Log.e("TAG", "messages:onCancelled: ${error.message}")
                }
            }
            locationRef.addValueEventListener(locationListener)
        }

        // [END basic_listen]
    }

    companion object {

        fun start(context: Context, data: String, type: Int) {
            val intent = Intent(context, ListActivity::class.java)
            intent.putExtra("type", type)
            intent.putExtra("data", data)
            context.startActivity(intent)
        }
    }
}