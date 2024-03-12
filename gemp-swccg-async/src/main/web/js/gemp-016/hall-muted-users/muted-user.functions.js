const IS_MUTE_ENABLED = "isMuteEnabled";
const MUTED_USERS = "mutedUsers";

const USER_NAME_REGEX = new RegExp(/<b>(\S*):<\/b>/);

function getIsMuteEnabled() {
    return window.localStorage.getItem(IS_MUTE_ENABLED) ?? false;
}

function getMutedUsers() {
    const mutedUsers = JSON.parse(window.localStorage.getItem(MUTED_USERS)) ?? '';
    return mutedUsers ?? [];
}

function setMutedUsers(userNames) {
    window.localStorage.setItem(MUTED_USERS, JSON.stringify(userNames));
}

function addMutedUser(userName) {
    const mutedUsers = getMutedUsers();
    if (mutedUsers.includes(userName)) {
        return;
    }
    setMutedUsers([...mutedUsers, userName]);
}

function getUserNameFromMessage(message) {
    const matches = USER_NAME_REGEX.exec(message);
    if (matches === null) {
        return '';
    }
    return matches[1];
}

function checkIfMessageShouldBeMuted(message) {
    // if (window.location.href.includes("game.html")) {
    //     return false;
    // }
    const userName = getUserNameFromMessage(message);
    console.log(userName)
    return getMutedUsers().includes(userName);
}