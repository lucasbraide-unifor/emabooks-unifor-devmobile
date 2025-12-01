package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AdminDashboardActivity : AppCompatActivity() {

    // Firestore
    private lateinit var fb: FirebaseFirestore

    private val COLLECTION_LIVROS = "livros"
    private val COLLECTION_RESERVAS = "reservas"
    private val COLLECTION_MULTAS = "multas"

    // Views
    private lateinit var toolbarAdmin: MaterialToolbar
    private lateinit var cardViewAcervo: MaterialCardView
    private lateinit var cardViewReservas: MaterialCardView
    private lateinit var cardViewMultas: MaterialCardView
    private lateinit var cardViewUsuarios: MaterialCardView
    private lateinit var cardViewConfiguracoes: MaterialCardView

    private lateinit var textViewLivrosValue: TextView
    private lateinit var textViewReservasValue: TextView
    private lateinit var textViewMultasValue: TextView

    // Listeners
    private var listenerLivros: ListenerRegistration? = null
    private var listenerReservas: ListenerRegistration? = null
    private var listenerMultas: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        fb = Firebase.firestore

        bindViews()
        setupToolbar()
        setupNavigationCards()
    }

    override fun onStart() {
        super.onStart()
        iniciarResumoEmTempoReal()
    }

    override fun onStop() {
        super.onStop()
        removerListeners()
    }

    private fun bindViews() {
        toolbarAdmin = findViewById(R.id.toolbarAdmin)

        cardViewAcervo = findViewById(R.id.cardViewAcervo)
        cardViewReservas = findViewById(R.id.cardViewReservas)
        cardViewMultas = findViewById(R.id.cardViewMultas)
        cardViewUsuarios = findViewById(R.id.cardViewUsuarios)
        cardViewConfiguracoes = findViewById(R.id.cardViewConfiguracoes)

        textViewLivrosValue = findViewById(R.id.textViewLivrosValue)
        textViewReservasValue = findViewById(R.id.textViewReservasValue)
        textViewMultasValue = findViewById(R.id.textViewMultasValue)
    }

    private fun setupToolbar() {
        toolbarAdmin.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupNavigationCards() {
        cardViewAcervo.setOnClickListener {
            startActivity(Intent(this, ActivityAdminAcervo::class.java))
        }

        cardViewReservas.setOnClickListener {
            startActivity(Intent(this, ActivityAdminReservas::class.java))
        }

        cardViewMultas.setOnClickListener {
            Toast.makeText(this, "Tela de Multas em desenvolvimento.", Toast.LENGTH_SHORT).show()
        }

        cardViewUsuarios.setOnClickListener {
            startActivity(Intent(this, ActivityAdminUsuarios::class.java))
        }

        cardViewConfiguracoes.setOnClickListener {
            startActivity(Intent(this, ActivityAdminConfiguracoes::class.java))
        }
    }

    private fun iniciarResumoEmTempoReal() {

        listenerLivros = fb.collection(COLLECTION_LIVROS)
            .addSnapshotListener { snap, error ->
                textViewLivrosValue.text = if (error != null) "—" else (snap?.size() ?: 0).toString()
            }

        listenerReservas = fb.collection(COLLECTION_RESERVAS)
            .whereEqualTo("status", "PENDENTE")
            .addSnapshotListener { snap, error ->
                textViewReservasValue.text = if (error != null) "—" else (snap?.size() ?: 0).toString()
            }

        listenerMultas = fb.collection(COLLECTION_MULTAS)
            .whereEqualTo("status", "ABERTA")
            .addSnapshotListener { snap, error ->
                textViewMultasValue.text = if (error != null) "—" else (snap?.size() ?: 0).toString()
            }
    }

    private fun removerListeners() {
        listenerLivros?.remove()
        listenerReservas?.remove()
        listenerMultas?.remove()
    }
}