package com.nestmate.app.ui.screens.community

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.ui.components.NestMatePrimaryButton

@Composable
fun PostDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    var newComment by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Mock data mapping
        Text(text = "Anyone looking for a flatmate in Vishrambag?", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "by Gautam", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "We have a 2BHK and need one more person to split the rent. 3000/month.", style = MaterialTheme.typography.bodyLarge)
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Comments", style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            // Mock comments
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text = "Sam", style = MaterialTheme.typography.labelLarge)
                    Text(text = "I'm interested. Is it fully furnished?")
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newComment,
                onValueChange = { newComment = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add comment...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            NestMatePrimaryButton(text = "Post", modifier = Modifier.width(80.dp), onClick = { newComment = "" })
        }
    }
}
