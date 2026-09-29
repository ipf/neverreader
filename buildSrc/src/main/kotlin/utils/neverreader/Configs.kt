package utils.neverreader

/**
 * Build types the app actually registers. The flavor, signing-config and
 * flavor-dimension constants that used to sit here described a Play/team build
 * matrix this project no longer has: no flavors are declared, so they were
 * never read and only the buildType names below survived.
 */
object BuildTypes {
    const val DEBUG = "debug"
    const val UNSIGNED_RELEASE = "unsignedRelease"
}
