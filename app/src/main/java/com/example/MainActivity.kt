package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- Room Database ---
@Entity(tableName = "tasks_table")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val time: String,
    val isCompleted: Boolean = false
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks_table ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_tasks_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MainTaskViewModel(private val dao: TaskDao) : ViewModel() {
    val tasks: StateFlow<List<TaskEntity>> = dao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var searchQuery = MutableStateFlow("")
    var selectedFilter = MutableStateFlow("الكل")

    fun addTask(title: String, desc: String, time: String) {
        viewModelScope.launch { dao.insertTask(TaskEntity(title = title, description = desc, time = time)) }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch { dao.updateTask(task.copy(isCompleted = !task.isCompleted)) }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch { dao.updateTask(task) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { dao.deleteTask(task) }
    }
}

// --- Colors ---
val DarkBackground = Color(0xFF12131C)
val SurfaceDark = Color(0xFF1E1F2C)
val CardBackground = Color(0xFF27293D)
val AccentPurple = Color(0xFF8B5CF6)
val AccentPurpleLight = Color(0xFFA78BFA)
val TextWhite = Color(0xFFF3F4F6)
val TextMuted = Color(0xFF9CA3AF)
val SuccessGreen = Color(0xFF10B981)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AppDatabase.getDatabase(this)
        val viewModel = MainTaskViewModel(db.taskDao())

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
                    var currentScreen by remember { mutableStateOf("welcome") }

                    AnimatedContent(targetState = currentScreen, label = "screen_trans") { screen ->
                        when (screen) {
                            "welcome" -> WelcomeScreen(onStartClick = { currentScreen = "home" })
                            "home" -> HomeScreen(viewModel = viewModel, onNavigateToTasks = { currentScreen = "tasks" })
                            "tasks" -> TaskListScreen(viewModel = viewModel, onBackClick = { currentScreen = "home" })
                        }
                    }
                }
            }
        }
    }
}

// --- 1. Welcome Screen ---
@Composable
fun WelcomeScreen(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.EventNote, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("سلسلة تطبيقات الذكي", fontSize = 16.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(8.dp))
        Text("الذكي | جدول المهام الذكي", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onStartClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("البدء الآن 🚀", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// --- 2. Home Screen ---
@Composable
fun HomeScreen(viewModel: MainTaskViewModel, onNavigateToTasks: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(40.dp))
            Text("الرئيسية", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text("إدارة و جدولة مهامك اليومية الذكية", fontSize = 14.sp, color = TextMuted)
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إضافة مهمة جديدة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = onNavigateToTasks,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = AccentPurpleLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text("قائمة المهام المسجلة 📋 (${tasks.size})", fontSize = 18.sp, color = TextWhite)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showAddDialog) {
        TaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, time ->
                viewModel.addTask(title, desc, time)
                showAddDialog = false
            }
        )
    }
}

// --- 3. Task List Screen ---
@Composable
fun TaskListScreen(viewModel: MainTaskViewModel, onBackClick: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextWhite) }
            Text("جدول المهام التفصيلي 📋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث في المهام...", color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks.filter { it.title.contains(searchQuery, ignoreCase = true) }) { task ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = task.isCompleted, onCheckedChange = { viewModel.toggleTask(task) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(task.time, fontSize = 12.sp, color = AccentPurpleLight)
                        }
                        IconButton(onClick = { viewModel.deleteTask(task) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        }
                    }
                }
            }
        }

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Text("إضافة مهمة جديدة +", color = Color.White)
        }
    }

    if (showAddDialog) {
        TaskDialog(onDismiss = { showAddDialog = false }, onConfirm = { t, d, time -> viewModel.addTask(t, d, time); showAddDialog = false })
    }
}

// --- Custom TimePicker Dialog ---
@Composable
fun TaskDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("08") }
    var minute by remember { mutableStateOf("00") }
    var amPm by remember { mutableStateOf("صباحاً") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مهمة جديدة", color = TextWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("وصف المهمة") })
                Text("منتقي الوقت (TimePicker):", color = TextMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { hour = if (hour == "12") "01" else String.format("%02d", hour.toInt() + 1) }) { Text(hour) }
                    Text(":", color = TextWhite, fontSize = 20.sp)
                    Button(onClick = { minute = if (minute == "45") "00" else String.format("%02d", minute.toInt() + 15) }) { Text(minute) }
                    Button(onClick = { amPm = if (amPm == "صباحاً") "مساءً" else "صباحاً" }) { Text(amPm) }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onConfirm(title, "", "$hour:$minute $amPm") }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        containerColor = SurfaceDark
    )
}
