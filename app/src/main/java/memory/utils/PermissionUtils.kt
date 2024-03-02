package memory.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.memorygame.CreateActivity

fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

fun requestPermission(activity: Activity?, permission: String, requestCode: Int) {
    //Log.i(CreateActivity.TAG, "Clicked")
    ActivityCompat.requestPermissions(activity!!, arrayOf(permission), requestCode)
}