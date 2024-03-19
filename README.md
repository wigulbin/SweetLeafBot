# Sweet Leaf Bot
 
Discord bot to aid with party signups for various activities for the game Palia.
  - Bot is designed to work in just one server at a time
  - **mvn package** to create uber-jar that can be run as is.
     - must set following environment variables:
        - guild_id: Discord channel id
        - token: Discord API key

**GCloud**
  - Debian box
  - Jar is run as a service on the machine
  - Currently running on Google Cloud Compute
     - Github actions build jar
     - gcloud compute scp
        - Copies jar into tmp directory on server
        - Forked version of the gcloud compute ssh action
     - gcloud compute ssh
        - Stops service, moves current jar into backup dir, copies tmp into main dir, starts service
        

**Docker**
  - Works with Build/Compose, unless containers are mounted seperately, updating image will remove the PartyInfo.txt file that's used as the storage for the party embeds (May refactor to use redis), if you dont ever plan on updating then this should not be an issue. Compose creates/mounts these volumes to persist through image updates. Note, docker support was added for the purpose of learning the technology, has not been tested outside my local environment as of the writing of this.
  - **Build:**
    - docker build --build-arg guild_id=${Your Guild ID} --build-arg token=${Your Discord API Key} -t sweetleafbot:1.0 .
    - Volumes are commented out of docker file due to compose down below
  - **Compose:**
    - docker compose --env-file .env up
    - empty.env is provided to fill in with the guild_id and token to pass through to the env variables


**Commands**
- **/party** - Creates an embedded message to set up a Palia party
    - **Parameters:**
      - **type** - Required. Options are Bug Catching, Hunting, Fishing, Mining, Cooking, or Custom
      - **server** - Not Required. Options are NA, EU, or PA
      - **people** - Not Required. Max number of people allowed for a party, *not* including Cooking.
      - **timestamp** - Not Required. Hammertime timestamp used for when the party is happening. Paste the entire Hammertime code into this field.
      - **voice** - Not Required. Options are true for Voice Chat Requested, or false for Voice Chat Not Required.
      - **recipe** - Not Required. Options are recipes used in Cooking parties. Should *only* be used for Cooking parties.
      - **quantity** - Not Required. The number of food being made in a Cooking party. Should *only* be used for Cooking parties.
- **/removeuser** - Returns a private dropdown message to select the user being removed. It will remove every instance of the user's name from the party.
    - **Parameters:**
        - **partyid** - Required. Options are the parties that you have created. Admins/Mods will see *all* parties.
- **/mentionparty** - Returns a public message that pings every user in the party.
  - **Parameters:**
      - **partyid** - Required. Options are the parties that you have created. Admins/Mods will see *all* parties.
- **/closeparty** - Returns a public message that pings every user in the party, removes all buttons from the embedded party message, and sets the embed to (Closed).
  - **Parameters:**
      - **partyid** - Required. Options are the parties that you have created. Admins/Mods will see *all* parties.

**Embedded Party Message**
- **Buttons:**
    - **Sign Up!** - When clicked, user's name will show under "Participants" in embed. Does not display for Cooking parties.
    - **Recipe Roles** - When clicked, user's name will show under respective recipe role in embed. Only displays for Cooking parties.
    - **Remove Name** - When clicked, all instances of user's name will be removed from embed. See **/removeuser** to remove someone other than yourself.