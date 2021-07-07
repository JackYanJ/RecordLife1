# RecordLife
A short Android client project based on [Kotlin][1] language, using the MVVM architecture implemented by [Jetpack][3].
## Brief Introduction
This is an app you can write a short note at your current loction. It uses Firebase as a back-end server and Google Map services to draw the map.
## The main function
- Locate user's current position.
- Add short note at current location displayed as a marker on the map.
- Turn to Google Map to direct to the marker position.
- Search other users' notes.

## Data Solutions
Firebase realtime database is using JSON tree pattern. It's not quite suitable for array data. 
You need use push method to add the new element.

## Express one's thanks
- [FireBase][1] FireBase Authentication and Realtime Database
- [Goole Map][2] Map Services 

[1]:https://firebase.google.com/
[2]:https://www.kaiyanapp.com
