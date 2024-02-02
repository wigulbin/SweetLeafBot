Discord bot to aid with party signups for various activities for the game Palia.
  - Bot is designed to work in just one server at a time
  - **mvn package** to create uber-jar that can be run as is.

**Docker:**
  Works with Build/Compose, unless containers are mounted seperately, updating image will remove the PartyInfo.txt file that's used as the storage for the party embeds (May refactor to use redis), if you dont ever plan on updating then this should not be an issue. Compose creates/mounts these volumes to persist through image updates. Note, docker support was added for the purpose of learning the technology, has not been tested outside my local environment as of the writing of this.
  - **Build:**
    - docker build --build-arg guild_id=${Your Guild ID} --build-arg token=${Your Discord API Key} -t sweetleafbot:1.0 .
    - Volumes are commented out of docker file due to compose down below
  - **Compose:**
    - docker compose --env-file .env up
    - empty.env is provided to fill in with the guild_id and token to pass through to the env variables
