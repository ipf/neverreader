package com.neverreader.util.android

import android.os.Parcel
import android.os.Parcelable

/**
 * Boilerplate for a parcelable class
 */
class ParcelableTemplate  // Parcelling
    (`in`: Parcel?) : Parcelable {
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
    }

    companion object {
        val CREATOR: Parcelable.Creator<ParcelableTemplate?> =
            object : Parcelable.Creator<ParcelableTemplate?> {
                override fun createFromParcel(`in`: Parcel): ParcelableTemplate {
                    return ParcelableTemplate(`in`)
                }

                override fun newArray(size: Int): Array<ParcelableTemplate?> {
                    return arrayOfNulls<ParcelableTemplate>(size)
                }
            }
    }
}
