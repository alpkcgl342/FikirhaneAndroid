package com.alpkcgl.fikirhane.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Author(
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
)

@Serializable
data class CategoryRef(val id: String, val name: String, val slug: String)

@Serializable
data class Category(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
)

@Serializable
enum class PostStatus { DRAFT, PUBLISHED }

/** Liste öğesi (`GET /posts`): içerik yerine düz metin özet. */
@Serializable
data class PostSummary(
    val id: String,
    val slug: String,
    val title: String,
    val excerpt: String = "",
    val coverUrl: String? = null,
    val status: PostStatus,
    val readingTime: Int,
    val createdAt: String,
    val updatedAt: String,
    val author: Author,
    val category: CategoryRef? = null,
    val tags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
)

/** Yazı detayı (`GET /posts/:slug`): Markdown içerik ve izleyiciye özel alanlar. */
@Serializable
data class PostDetail(
    val id: String,
    val slug: String,
    val title: String,
    val content: String,
    val coverUrl: String? = null,
    val status: PostStatus,
    val readingTime: Int,
    val createdAt: String,
    val updatedAt: String,
    val author: Author,
    val category: CategoryRef? = null,
    val tags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isOwner: Boolean = false,
    val likedByMe: Boolean = false,
    val bookmarkedByMe: Boolean = false,
)

@Serializable
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
)

@Serializable
data class CategoriesResponse(val items: List<Category>)

/** Oluşturma ve güncelleme gövdesi; tüm alanlar gönderilir (`null` = kaldır). */
@Serializable
data class PostInput(
    val title: String,
    val content: String,
    val categoryId: String?,
    val tags: List<String>,
    val coverUrl: String?,
    val status: PostStatus,
)

@Serializable
data class UploadResponse(val url: String)

/** `GET /posts` filtreleri; `status` DRAFT/ALL yalnızca kendi yazıları döner. */
data class PostQuery(
    val status: String? = null,
    val author: String? = null,
    val category: String? = null,
    val tag: String? = null,
)
