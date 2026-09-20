package com.neverreader.sdk.preferences

import com.neverreader.util.prefs.LongPref
import com.neverreader.util.prefs.MemoryPrefStore

class InMemoryLongPreference : LongPref("key", 0L, MemoryPrefStore())
