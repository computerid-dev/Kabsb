package com.echochat.cid.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.echochat.cid.R
import com.echochat.cid.data.FirestoreRepository
import com.echochat.cid.databinding.ActivityMainBinding
import com.echochat.cid.util.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Daftarkan/perbarui keberadaan UID setiap kali aplikasi dibuka,
        // supaya teman bisa menemukan kita lewat kode ID.
        val session = SessionManager(this)
        FirestoreRepository().registerPresence(
            session.myUid, session.displayName, session.avatarBase64
        )

        if (savedInstanceState == null) {
            showFragment(ChatsFragment())
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.tabChats -> ChatsFragment()
                R.id.tabContacts -> ContactsFragment()
                R.id.tabAccount -> AccountFragment()
                else -> return@setOnItemSelectedListener false
            }
            showFragment(fragment)
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
