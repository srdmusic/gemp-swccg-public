const mutedUsersDialog = `
<div id="muted-users" title="Muted Users">
    <table>
        <th>Username</th>
        <th>Reason</th>
        <th>End Date</th>
        <tbody id="muted-users-table-body"></tbody>
    </table>
</div>
`;

const mutedUserRow = (user) => `<tr><td>${user}</td><td>${user}</td><td>${user}</td></tr>`;
 function buildMutedUsersDialog() {
    $('#foobar').html(mutedUsersDialog);
    $('#foobar').dialog({
        autoOpen: false,
        width: 600,
        title: 'Muted Users',
        zIndex: 10000,
        buttons: {
            "Close": function () {
                $(this).dialog("close");
            }
        }
    });
    }


function showMutedUsersDialog() {
    var mutedUsers = getMutedUsers();
    for (var i = 0; i < mutedUsers.length; i++) {
        var user = mutedUsers[i];
        $('#muted-users-table-body').append(mutedUserRow(user));
    }
    $('#foobar').dialog("open");
}