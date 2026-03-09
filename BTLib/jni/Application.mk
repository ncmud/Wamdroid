APP_ABI := arm64-v8a x86_64
APP_PLATFORM := android-31
APP_OPTIM := release
APP_CFLAGS := -O3
APP_MODULES := lua lsqlite3 sqlite3 bit marshal luabins
#above, lua really means luajava in the luajava subdirectory
