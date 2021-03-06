package com.habitrpg.android.habitica.models.tasks

import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.StringDef
import com.google.gson.annotations.SerializedName
import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.models.Tag
import com.habitrpg.android.habitica.models.user.Stats
import com.habitrpg.android.habitica.ui.helpers.MarkdownParser
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import org.json.JSONArray
import org.json.JSONException
import java.util.*

open class Task : RealmObject, Parcelable {


    var userId: String = ""
    var priority: Float = 0.0f
    var text: String = ""
    var notes: String = ""
    @TaskTypes
    var type: String = ""
    @Stats.StatsTypes
    var attribute: String = Stats.STRENGTH
    var value: Double = 0.0
    var tags: RealmList<Tag> = RealmList()
    var dateCreated: Date? = null
    var position: Int = 0
    var group: TaskGroupPlan? = null
    //Habits
    var up: Boolean = false
    var down: Boolean = false
    var counterUp: Int = 0
    var counterDown: Int = 0
    //todos/dailies
    var completed: Boolean = false
    var checklist: RealmList<ChecklistItem> = RealmList()
    var reminders: RealmList<RemindersItem> = RealmList()
    //dailies
    var frequency: String? = null
    var everyX: Int = 0
    var streak: Int = 0
    var startDate: Date? = null
    var repeat: Days? = null
    //todos
    @SerializedName("date")
    var dueDate: Date? = null
    //TODO: private String lastCompleted;
    // used for buyable items
    var specialTag: String = ""
    @Ignore
    var parsedText: CharSequence? = null
    @Ignore
    var parsedNotes: CharSequence? = null
    @PrimaryKey
    @SerializedName("_id")
    var id: String? = null

    var isDue: Boolean? = null

    var nextDue: Date? = null
    var yesterDaily: Boolean? = null

    private var daysOfMonthString: String? = null
    private var weeksOfMonthString: String? = null

    @Ignore
    private var daysOfMonth: MutableList<Int>? = null
    @Ignore
    private var weeksOfMonth: MutableList<Int>? = null

    val completedChecklistCount: Int
        get() = checklist.count { it.completed }

    val lightTaskColor: Int
        get() {
            return when {
                this.value < -20 -> return R.color.maroon_100
                this.value < -10 -> return R.color.red_100
                this.value < -1 -> return R.color.orange_100
                this.value < 1 -> return R.color.yellow_100
                this.value < 5 -> return R.color.green_100
                this.value < 10 -> return R.color.teal_100
                else -> R.color.blue_100
            }
        }

    val mediumTaskColor: Int
        get() {
            return when {
                this.value < -20 -> return R.color.maroon_50
                this.value < -10 -> return R.color.red_50
                this.value < -1 -> return R.color.orange_50
                this.value < 1 -> return R.color.yellow_50
                this.value < 5 -> return R.color.green_50
                this.value < 10 -> return R.color.teal_50
                else -> R.color.blue_50
            }
        }

    val darkTaskColor: Int
        get() {
            return when {
                this.value < -20 -> return R.color.maroon_10
                this.value < -10 -> return R.color.red_10
                this.value < -1 -> return R.color.orange_10
                this.value < 1 -> return R.color.yellow_10
                this.value < 5 -> return R.color.green_10
                this.value < 10 -> return R.color.teal_10
                else -> R.color.blue_10
            }
        }

    val isDisplayedActive: Boolean
        get() = isDue == true && !completed

    val isChecklistDisplayActive: Boolean
        get() = this.isDisplayedActive && this.checklist.size != this.completedChecklistCount

    val isGroupTask: Boolean
        get() = group?.approvalApproved == true

    val isPendingApproval: Boolean
        get() = (group?.approvalRequired == true && group?.approvalRequested == true && group?.approvalApproved == false)

    @StringDef(TYPE_HABIT, TYPE_DAILY, TYPE_TODO, TYPE_REWARD)
    @Retention(AnnotationRetention.SOURCE)
    annotation class TaskTypes

    fun containsAllTagIds(tagIdList: List<String>): Boolean = tags.mapTo(ArrayList()) { it.getId() }.containsAll(tagIdList)

    fun checkIfDue(): Boolean? = isDue == true

    fun getNextReminderOccurence(oldTime: Date): Date? {
        val today = Calendar.getInstance()

        val newTime = GregorianCalendar()
        newTime.time = oldTime
        newTime.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
        if (today.before(newTime)) {
            today.add(Calendar.DAY_OF_MONTH, -1)
        }

        if (nextDue != null && !isDisplayedActive) {
            val nextDueCalendar = GregorianCalendar()
            nextDueCalendar.time = nextDue
            newTime.set(nextDueCalendar.get(Calendar.YEAR), nextDueCalendar.get(Calendar.MONTH), nextDueCalendar.get(Calendar.DAY_OF_MONTH))
            return newTime.time
        }

        return newTime.time
    }

    fun parseMarkdown() {
        try {
            this.parsedText = MarkdownParser.parseMarkdown(this.text)
        } catch (e: NullPointerException) {
            this.parsedText = this.text
        }

        try {
            this.parsedNotes = MarkdownParser.parseMarkdown(this.notes)
        } catch (e: NullPointerException) {
            this.parsedNotes = this.notes
        }

    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (Task::class.java.isAssignableFrom(other.javaClass)) {
            val otherTask = other as Task
            return this.id == otherTask.id
        }
        return super.equals(other)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.userId)
        dest.writeValue(this.priority)
        dest.writeString(this.text)
        dest.writeString(this.notes)
        dest.writeString(this.attribute)
        dest.writeString(this.type)
        dest.writeDouble(this.value)
        dest.writeList(this.tags)
        dest.writeLong(if (this.dateCreated != null) this.dateCreated!!.time else -1)
        dest.writeInt(this.position)
        dest.writeValue(this.up)
        dest.writeValue(this.down)
        dest.writeByte(if (this.completed) 1.toByte() else 0.toByte())
        dest.writeList(this.checklist)
        dest.writeList(this.reminders)
        dest.writeString(this.frequency)
        dest.writeValue(this.everyX)
        dest.writeValue(this.streak)
        dest.writeLong(if (this.startDate != null) this.startDate!!.time else -1)
        dest.writeParcelable(this.repeat, flags)
        dest.writeLong(if (this.dueDate != null) this.dueDate!!.time else -1)
        dest.writeString(this.specialTag)
        dest.writeString(this.id)
    }

    constructor() {}

    protected constructor(`in`: Parcel) {
        this.userId = `in`.readString()
        this.priority = `in`.readValue(Float::class.java.classLoader) as Float
        this.text = `in`.readString()
        this.notes = `in`.readString()
        this.attribute = `in`.readString()
        this.type = `in`.readString()
        this.value = `in`.readDouble()
        this.tags = RealmList()
        `in`.readList(this.tags, TaskTag::class.java.classLoader)
        val tmpDateCreated = `in`.readLong()
        this.dateCreated = if (tmpDateCreated == -1L) null else Date(tmpDateCreated)
        this.position = `in`.readInt()
        this.up = `in`.readValue(Boolean::class.java.classLoader) as Boolean
        this.down = `in`.readValue(Boolean::class.java.classLoader) as Boolean
        this.completed = `in`.readByte().toInt() != 0
        this.checklist = RealmList()
        `in`.readList(this.checklist, ChecklistItem::class.java.classLoader)
        this.reminders = RealmList()
        `in`.readList(this.reminders, RemindersItem::class.java.classLoader)
        this.frequency = `in`.readString()
        this.everyX = `in`.readValue(Int::class.java.classLoader) as Int
        this.streak = `in`.readValue(Int::class.java.classLoader) as Int
        val tmpStartDate = `in`.readLong()
        this.startDate = if (tmpStartDate == -1L) null else Date(tmpStartDate)
        this.repeat = `in`.readParcelable(Days::class.java.classLoader)
        val tmpDuedate = `in`.readLong()
        this.dueDate = if (tmpDuedate == -1L) null else Date(tmpDuedate)
        this.specialTag = `in`.readString()
        this.id = `in`.readString()
    }


    fun setWeeksOfMonth(weeksOfMonth: MutableList<Int>) {
        this.weeksOfMonth = weeksOfMonth
        this.weeksOfMonthString = this.weeksOfMonth!!.toString()
    }

    fun getWeeksOfMonth(): List<Int>? {
        if (weeksOfMonth == null) {
            weeksOfMonth = ArrayList()
            if (weeksOfMonthString != null) {
                try {
                    val obj = JSONArray(weeksOfMonthString)
                    var i = 0
                    while (i < obj.length()) {
                        weeksOfMonth!!.add(obj.getInt(i))
                        i += 1
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            } else {
                weeksOfMonth = ArrayList()
            }
        }
        return weeksOfMonth
    }

    fun setDaysOfMonth(daysOfMonth: MutableList<Int>) {
        this.daysOfMonth = daysOfMonth
        this.daysOfMonthString = this.daysOfMonth!!.toString()
    }

    fun getDaysOfMonth(): List<Int>? {
        if (daysOfMonth == null) {
            daysOfMonth = ArrayList()
            if (daysOfMonthString != null) {
                try {
                    val obj = JSONArray(daysOfMonthString)
                    var i = 0
                    while (i < obj.length()) {
                        daysOfMonth!!.add(obj.getInt(i))
                        i += 1
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            } else {
                daysOfMonth = ArrayList()
            }
        }

        return daysOfMonth
    }

    companion object {
        const val TYPE_HABIT = "habit"
        const val TYPE_TODO = "todo"
        const val TYPE_DAILY = "daily"
        const val TYPE_REWARD = "reward"

        const val FILTER_ALL = "all"
        const val FILTER_WEAK = "weak"
        const val FILTER_STRONG = "strong"
        const val FILTER_ACTIVE = "active"
        const val FILTER_GRAY = "gray"
        const val FILTER_DATED = "dated"
        const val FILTER_COMPLETED = "completed"
        const val FREQUENCY_WEEKLY = "weekly"
        const val FREQUENCY_DAILY = "daily"

        val CREATOR: Parcelable.Creator<Task> = object : Parcelable.Creator<Task> {
            override fun createFromParcel(source: Parcel): Task = Task(source)

            override fun newArray(size: Int): Array<Task?> = arrayOfNulls(size)
        }
    }
}
