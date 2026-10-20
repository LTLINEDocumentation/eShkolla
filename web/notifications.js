window.refreshNotificationBadge = async function refreshNotificationBadge(sourceItems){
  try {
    const items = sourceItems || await api('/api/v1/notifications');
    const unread = items.filter(n=>!n.isRead).length;
  await window.refreshNotificationBadge?.(items);
    const badge = $('notificationsBadge');
    const cardBadge = $('notificationCardBadge');
    [badge,cardBadge].filter(Boolean).forEach(el=>{el.textContent=unread>99?'99+':String(unread);el.classList.toggle('hidden',unread===0);});
    const shortcut = $('notificationsShortcut');
    if(shortcut) shortcut.setAttribute('aria-label',unread?`Njoftimet, ${unread} të palexuara`:'Njoftimet');
    return unread;
  } catch(e) { console.warn('Nuk u lexua numri i njoftimeve',e); return null; }
};

window.renderNotifications = async function renderNotifications(){
  const items = await api('/api/v1/notifications');
  const canPublish = ['ADMINISTRATOR','DREJTOR'].includes(currentUser?.role);
  const unread = items.filter(n=>!n.isRead).length;
  const audienceLabels = {ALL:'Të gjithë',ADMINISTRATOR:'Administratorët',DREJTOR:'Drejtorët',MESIMDHENES:'Mësimdhënësit',PRIND:'Prindërit',NXENES:'Nxënësit'};
  $('moduleContent').innerHTML = `
    ${canPublish ? `<form id="notificationComposer" class="notification-composer">
      <h4>Publiko njoftim të përgjithshëm</h4>
      <label>Destinacioni<select name="audienceRole" required>
        <option value="ALL">Të gjithë përdoruesit</option>
        <option value="MESIMDHENES">Vetëm mësimdhënësit</option>
        <option value="PRIND">Vetëm prindërit</option>
        <option value="NXENES">Vetëm nxënësit</option>
        <option value="DREJTOR">Vetëm drejtorët</option>
        <option value="ADMINISTRATOR">Vetëm administratorët</option>
      </select></label>
      <label>Titulli<input name="title" maxlength="180" required placeholder="Titulli i njoftimit"></label>
      <label>Përmbajtja<textarea name="message" rows="3" maxlength="4000" required placeholder="Shkruani njoftimin..."></textarea></label>
      <div class="notification-form-actions"><button class="primary" type="submit">Publiko njoftimin</button></div>
      <p id="notificationFormError" class="error"></p>
    </form>` : ''}
    <div class="notification-list-head">
      <div><strong>Njoftimet e tua</strong><small>${unread} të palexuara · shfaqen deri në 200 njoftimet më të fundit</small></div>
      ${unread ? '<button type="button" class="secondary" id="notificationReadAll">Shënoji të gjitha si të lexuara</button>' : ''}
    </div>
    <div class="notification-list">
      ${items.length ? items.map(n=>`<article class="notification-item ${n.isRead?'is-read':'is-unread'}">
        <div class="notification-item-icon">${n.type==='ANNOUNCEMENT'?'📣':n.entityType==='GRADE'?'⭐':n.entityType==='ABSENCE'?'🕒':'🔔'}</div>
        <div class="notification-item-content">
          <div class="notification-item-title"><strong>${esc(n.title)}</strong><span class="notification-status">${n.isRead?'Lexuar':'E re'}</span></div>
          <p>${esc(n.message)}</p>
          <small>${esc(n.createdByName)} · ${esc(new Date(n.createdAt).toLocaleString('sq-AL'))}${n.audienceRole?' · '+esc(audienceLabels[n.audienceRole]||n.audienceRole):''}</small>
        </div>
        ${!n.isRead?'<button type="button" class="secondary notification-mark-read" data-id="'+esc(n.id)+'">Lexova</button>':''}
      </article>`).join('') : '<div class="empty-state">Nuk ka njoftime për llogarinë tuaj.</div>'}
    </div>`;
  const composer=$('notificationComposer');
  if(composer) composer.onsubmit=async event=>{
    event.preventDefault();
    const data=new FormData(composer);
    const error=$('notificationFormError');
    error.textContent='';
    const button=composer.querySelector('button[type="submit"]');
    button.disabled=true;button.textContent='Duke publikuar...';
    try{
      await api('/api/v1/notifications',{method:'POST',body:JSON.stringify({title:data.get('title'),message:data.get('message'),audienceRole:data.get('audienceRole')})});
      await window.renderNotifications();
    }catch(e){error.textContent=e.message}
    finally{if(button.isConnected){button.disabled=false;button.textContent='Publiko njoftimin'}}
  };
  const readAll=$('notificationReadAll');
  if(readAll)readAll.onclick=async()=>{await api('/api/v1/notifications/read-all',{method:'POST'});await window.renderNotifications()};
  document.querySelectorAll('.notification-mark-read').forEach(button=>button.onclick=async()=>{button.disabled=true;try{await api('/api/v1/notifications/'+encodeURIComponent(button.dataset.id)+'/read',{method:'POST'});await window.renderNotifications()}catch(e){button.disabled=false;alert(e.message)}});
};
