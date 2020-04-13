package ro.htv.model

data class Post(
        var idpost:Int = 0,
        var ownwer_uid: String = "",
        var owner_name: String = "",
        var topic: String = "",
        var text: String = "",
        var timestamp: String = "",
        var linkToImage: String = "",
        var post: Boolean = true,
        var parent: String = ""
)