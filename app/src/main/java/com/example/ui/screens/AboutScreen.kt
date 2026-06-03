package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // This assumes you save the developer image as 'developer_photo' in res/drawable
        // For now, I will use a placeholder logic or ensure you rename/add it correctly.
        // I will instruct the user to ensure the file is named 'developer_photo.png' in res/drawable
        // This is a placeholder as the photo is unavailable.
        // Image(
        //     painter = painterResource(id = R.drawable.developer_photo),
        //     contentDescription = "Foto Pengembang",
        //     modifier = Modifier
        //         .size(150.dp)
        //         .clip(CircleShape),
        //     contentScale = ContentScale.Crop
        // )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Pengembang Aplikasi",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Panuluh Adi Riyanto",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No. HP: +6285290476699",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
