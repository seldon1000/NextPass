/*
 * Copyright 2021 Nicolas Mariniello
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.seldon1000.nextpass.ui.layout

sealed class Routes(val route: String) {
    object AccessPin : Routes(route = "access-pin/{shouldRaiseBiometric}") {
        fun getRoute(arg: Boolean) = "access-pin/$arg"
    }

    object WebView : Routes(route = "webview/{url}") {
        fun getRoute(arg: String) = "webview/$arg"
    }

    object Welcome : Routes(route = "welcome")
    object Search : Routes(route = "search")
    object Passwords : Routes(route = "passwords")
    object NewPassword : Routes(route = "new-password")
    object NewFolder : Routes(route = "new-folder")
    object Favorites : Routes(route = "favorites")
    object Settings : Routes(route = "settings")
    object About : Routes(route = "about")
    object PasswordDetails : Routes(route = "password-details/{data}") {
        fun getRoute(arg: Int) = "password-details/$arg"
    }

    object FolderDetails : Routes(route = "folder-details/{data}") {
        fun getRoute(arg: Int) = "folder-details/$arg"
    }

    object Pin : Routes(route = "pin/{change}") {
        fun getRoute(arg: Boolean) = "pin/$arg"
    }
}