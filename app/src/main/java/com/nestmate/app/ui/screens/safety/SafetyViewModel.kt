package com.nestmate.app.ui.screens.safety

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class EmergencyContact(val name: String, val phone: String)


@HiltViewModel
class SafetyViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("safety_prefs", Context.MODE_PRIVATE)

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        val serialized = prefs.getString("contacts", "") ?: ""
        if (serialized.isNotBlank()) {
            val list = serialized.split(";").mapNotNull {
                val parts = it.split("|")
                if (parts.size == 2) EmergencyContact(parts[0], parts[1]) else null
            }
            _contacts.value = list
        }
    }

    private fun saveContacts(list: List<EmergencyContact>) {
        val serialized = list.joinToString(";") { "${it.name}|${it.phone}" }
        prefs.edit().putString("contacts", serialized).commit() // synchronous — prevents race on back nav
        _contacts.value = list
    }

    fun addContact(name: String, phone: String) {
        if (_contacts.value.size < 5) {
            val updated = _contacts.value.toMutableList()
            updated.add(EmergencyContact(name, phone))
            saveContacts(updated)
        }
    }

    fun removeContact(contact: EmergencyContact) {
        val updated = _contacts.value.toMutableList()
        updated.remove(contact)
        saveContacts(updated)
    }

    fun triggerSOS(context: Context) {
        val message = "SOS! Emergency alert from NestMate. I need help. My last known location is College Campus."
        val currentContacts = _contacts.value

        if (currentContacts.isEmpty()) {
            // Fallback to standard share chooser if no contacts saved
            fallbackSosShare(context, message)
            return
        }

        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }

            var smsSent = false
            for (contact in currentContacts) {
                try {
                    smsManager?.sendTextMessage(contact.phone, null, message, null, null)
                    smsSent = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (!smsSent) {
                fallbackSosShare(context, message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackSosShare(context, message)
        }
    }

    private fun fallbackSosShare(context: Context, message: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(intent, "Share SOS message via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
