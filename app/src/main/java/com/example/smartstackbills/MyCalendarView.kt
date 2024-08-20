import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.smartstackbills.AboutUs
import com.example.smartstackbills.Datasecurity
import com.example.smartstackbills.FAQs
import com.example.smartstackbills.Help
import com.example.smartstackbills.LogIn
import com.example.smartstackbills.MainMenu
import com.example.smartstackbills.MyBills
import com.example.smartstackbills.MyIncome
import com.example.smartstackbills.MySpendings
import com.example.smartstackbills.R
import com.example.smartstackbills.Terms
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MyCalendarView : AppCompatActivity() {
    private var userEmail: String? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var calendarView: CalendarView
    private lateinit var selectedDateTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_view)

        userEmail = intent.getStringExtra("USER_EMAIL")
        drawerLayout = findViewById(R.id.drawer_layout)  // Initialize drawerLayout
        calendarView = findViewById(R.id.calendarView)
        selectedDateTextView = findViewById(R.id.selectedDateTextView)

        // Configurar el listener para manejar la selección de la fecha
        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            val dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)
            selectedDateTextView.text = "Fecha seleccionada: $dateString"
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationViewCalendar)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Main -> {
                    val intent = Intent(this, MainMenu::class.java)
                    startActivity(intent)
                    true
                }
                R.id.Bills -> {
                    val intent = Intent(this, MyBills::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Spendings -> {
                    val intent = Intent(this, MySpendings::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Income -> {  // New navigation option for Income
                    val intent = Intent(this, MyIncome::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                R.id.Calendar -> {
                    val intent = Intent(this,calendarView::class.java)
                    intent.putExtra("USER_EMAIL", userEmail)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        // Initialize the toolbar and set it as the action bar
        val toolbar: Toolbar = findViewById(R.id.toolbar_main_Calendar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }


        // Setup NavigationView
        val navView: NavigationView = findViewById(R.id.nav_view_Calendar)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_item_aboutus -> {
                    val intent = Intent(this, AboutUs::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_item_faq -> {
                    val intent = Intent(this, FAQs::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_item_datasec -> {
                    val intent = Intent(this, Datasecurity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_item_help -> {
                    val intent = Intent(this, Help::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_item_terms -> {
                    val intent = Intent(this, Terms::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_item_logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }

    }
    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}