package ro.htv.utils

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import ro.htv.model.Response
import ro.htv.model.User

class FirestoreRepository {

    private val TAG = "HackTheVirus FirestoreRepository"
    private val root: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val usersCollection = root.collection("users")

    fun createUser(user: User): MutableLiveData<Response> {
        val response = MutableLiveData<Response>()

        usersCollection.document(user.uid).set(user)
                .addOnSuccessListener {
                    response.value = Response(Utils.Responses.OK, user.name)
                }.addOnFailureListener {
                    response.value = Response(Utils.Responses.ERROR, it.message)
                }

        return response
    }

    fun setImage(uid: String, uri: String): MutableLiveData<Boolean> {
        // imi bag pula in el fail gracefully
        // is prea mici sansele sa esueze aici
        val response = MutableLiveData<Boolean>()

        usersCollection.document(uid).update("profileImage", uri)
                .addOnSuccessListener {
                    response.value = true
                }.addOnFailureListener {
                    response.value = false
                }

        return response
    }

    fun getTopics(): MutableLiveData<ArrayList<String>> {
        val response = MutableLiveData<ArrayList<String>>()
        val arr = ArrayList<String>()
        root.collection("topics").get()
                .addOnSuccessListener {
                    it.forEach { it2 ->
                        Log.d(TAG, it2.data["name"].toString())
                        arr.add(it2.data["name"].toString())
                        response.value = arr
                    }
                }
        return response
    }

    fun getPostsByTopic(topic: String): MutableLiveData<Response> {
        val response = MutableLiveData<Response>()

        root.collection("posts")
                .whereEqualTo("post", true)
                .whereEqualTo("topic", topic)
                .get()
                .addOnSuccessListener {
                    val arr = ArrayList<String>()
                    it.forEach { snapshot ->
                        Log.d(TAG, "asd ${snapshot.data.toMap()}")
                        arr.add(snapshot.data.toMap().toString())
                    }
                    if (arr.isNotEmpty()) {
                        response.value = Response(
                                Utils.Responses.OK,
                                arr
                        )
                    } else {
                        response.value = Response(
                                Utils.Responses.ERROR,
                                Utils.Errors.EMPTY
                        )
                    }
                }.addOnFailureListener {
                    response.value = Response(
                            Utils.Responses.ERROR,
                            it.message
                    )
                }

        return response
    }
}