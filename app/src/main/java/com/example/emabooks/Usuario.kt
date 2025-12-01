package com.example.emabooks

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Usuario(

    // id do documento no Firestore
    @DocumentId
    var id: String? = null,

    var nomeCompleto: String? = null,
    var email: String? = null,
    var matricula: String? = null,

    // se precisar armazenar a senha aqui, deixe como nullable
    var senha: String? = null,

    // campo para segmentar na tela de login
    @get:PropertyName("isAdmin") @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,

    // pra controlar "Ativo / Inativo" na tela de gestão
    @get:PropertyName("ativo") @set:PropertyName("ativo")
    var ativo: Boolean = true,

    // campos opcionais pra auditoria
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)