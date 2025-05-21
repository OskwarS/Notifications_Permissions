package com.example.zad5

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val CHANNEL_ID = "default_channel"
        const val NOTIFICATION_ID = 1
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            NotificationApp()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Default Channel"
            val descriptionText = "This is the default notification channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun NotificationApp() {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var expandedDescription by remember { mutableStateOf("") }
        val alarmHour by remember { mutableStateOf(7) }
        val alarmMinutes by remember { mutableStateOf(0) }
        val icons = listOf(
            R.drawable.clock1,
            R.drawable.clock800
        )
        var selectedIcon by remember { mutableStateOf(icons[0]) }
        var selectedStyle by remember { mutableStateOf("Default") }
        var permissionGranted by remember { mutableStateOf(checkNotificationPermission()) }

        val requestPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionGranted = isGranted
            if (!isGranted) {
                Toast.makeText(
                    this@MainActivity,
                    "Brak zezwolenia! Nie można wyświetlić powiadomienia.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
                    .padding(8.dp),
                decorationBox = {
                    if (title.isEmpty()) Text("Wprowadź tytuł")
                    it()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            BasicTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
                    .padding(8.dp),
                decorationBox = {
                    if (description.isEmpty()) Text("Wprowadź opis")
                    it()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            BasicTextField(
                value = expandedDescription,
                onValueChange = { expandedDescription = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
                    .padding(8.dp),
                decorationBox = {
                    if (expandedDescription.isEmpty()) Text("Wprowadź poszerzony opis")
                    it()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Wybierz ikonę:")
            icons.forEach { icon ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (selectedIcon == icon),
                            onClick = { selectedIcon = icon },
                            role = Role.RadioButton
                        )
                        .padding(8.dp)
                ) {
                    RadioButton(
                        selected = (selectedIcon == icon),
                        onClick = { selectedIcon = icon }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Wybierz styl powiadomienia:")
            listOf("Default", "BigText", "BigPicture").forEach { style ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (selectedStyle == style),
                            onClick = { selectedStyle = style },
                            role = Role.RadioButton
                        )
                        .padding(8.dp)
                ) {
                    RadioButton(
                        selected = (selectedStyle == style),
                        onClick = { selectedStyle = style }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(style)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (permissionGranted) {
                    sendSimpleNotification(
                        this@MainActivity,
                        title,
                        description,
                        expandedDescription,
                        selectedIcon,
                        selectedStyle,
                        alarmHour,
                        alarmMinutes
                    )
                } else {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }) {
                Text("Wyślij Powiadomienie")
            }
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendSimpleNotification(
        context: Context,
        title: String,
        description: String,
        expandedDescription: String,
        @DrawableRes selectedIconId: Int,
        selectedStyle: String,
        hour: Int,
        minutes: Int
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(selectedIconId)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (selectedStyle == "BigText") {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedDescription)
            )
        } else if (selectedStyle == "BigPicture") {
            val bitmap = BitmapFactory.decodeResource(resources, selectedIconId)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
            )
        }

        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, "Budzik")
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
        }

        val alarmPendingIntent = PendingIntent.getActivity(
            context,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.addAction(
            R.drawable.clock1,
            "Ustaw nowy alarm",
            alarmPendingIntent
        )

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}