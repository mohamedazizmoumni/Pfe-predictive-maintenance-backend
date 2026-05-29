# Quick Start: Restart and Test Instructions

## ✅ Build Complete - Ready to Restart

All fixes have been implemented and compiled successfully. Follow these steps to test:

## Step 1: Restart the Application

```bash
# Stop the current running application (Ctrl+C if running in terminal)

# Start the application
mvn -f pom.xml -pl api-module -am spring-boot:run

# Or if you prefer running the JAR:
java -jar api-module/target/api-module-1.0.0.jar
```

## Step 2: Wait for Startup

Look for these log messages:
```
✅ Flyway migration V41 applied successfully
✅ Started PredictiveMaintenanceApplication
✅ Tomcat started on port(s): 8080
```

## Step 3: Test in Browser/Frontend

### Login as STOCK_MANAGER
- Username: `stockmanager1`
- Password: (your password)

### Expected Results:
1. **No 500 Errors** ✅
   - Console should be clean
   - No red errors about notifications

2. **Notification Bell Shows Count** ✅
   - Should show "2" (from seed data)
   - Click to see welcome notifications

3. **Stock Notifications Page Works** ✅
   - Navigate to "Stock Notifications"
   - Should display notifications

## Step 4: Test Stock Update

1. Go to Inventory → Parts
2. Select any part
3. Click "Edit"
4. Change stock to a low value (below minimum)
5. Save

**Expected:** New notification appears for low stock

## Step 5: Test Reorder Request

1. Go to Inventory → Reorder Requests
2. Click "Create Reorder"
3. Select a part and quantity
4. Submit

**Expected:** 
- Reorder created successfully
- Notification sent to managers

## Step 6: Test Reorder Approval (as Manager)

1. Logout from STOCK_MANAGER
2. Login as MANAGER (manager1)
3. Go to Notifications
4. See the reorder request notification
5. Go to Reorder Requests
6. Approve or reject the request

**Expected:**
- Approval/rejection successful
- STOCK_MANAGER receives notification

## Quick API Tests (Optional)

### Test 1: Get Notifications
```bash
# Replace YOUR_TOKEN with actual JWT token
curl -X GET "http://localhost:8080/api/v1/notifications?role=STOCK_MANAGER" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Expected: 200 OK with array of notifications
```

### Test 2: Get Unread Count
```bash
curl -X GET "http://localhost:8080/api/v1/notifications/unread-count" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Expected: 200 OK {"count": 2}
```

### Test 3: Update Part Stock
```bash
curl -X PUT "http://localhost:8080/api/v1/inventory/parts/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "currentStock": 5,
    "minimumStock": 10
  }'

# Expected: 200 OK + new notification created
```

## Verification Checklist

- [ ] Application starts without errors
- [ ] V41 migration applied successfully
- [ ] Login as STOCK_MANAGER works
- [ ] No 500 errors in browser console
- [ ] Notification count shows (at least 2)
- [ ] Can view notifications
- [ ] Can mark notifications as read
- [ ] Stock update creates notification
- [ ] Reorder request creates notification
- [ ] Reorder approval creates notification

## If Something Goes Wrong

### Check Application Logs
```bash
tail -f api-module/logs/application.log
```

### Check Database
```sql
-- Verify migration ran
SELECT * FROM flyway_schema_history WHERE version = '41';

-- Check notifications exist
SELECT * FROM notifications WHERE target_roles LIKE '%STOCK_MANAGER%';

-- Check parts table
SELECT id, name, current_stock, minimum_stock, status FROM parts LIMIT 5;
```

### Common Issues

**Issue: Migration doesn't run**
- Solution: Check if V41 already exists in flyway_schema_history
- If needed, manually run the SQL from V41__seed_stock_notifications.sql

**Issue: Still getting 500 errors**
- Solution: Check application logs for specific error
- Verify STOCK_MANAGER role exists in database
- Check JWT token is valid

**Issue: Notifications not created**
- Solution: Check logs for "Created ... notification" messages
- Verify StockNotificationService is being called
- Check database for new notification entries

## Success Indicators

✅ **No console errors**  
✅ **Notification count visible**  
✅ **Can view and interact with notifications**  
✅ **Stock updates trigger notifications**  
✅ **Reorder workflow creates notifications**  
✅ **All roles can access their notifications**

## Next Steps After Testing

1. **Monitor for a few hours** - Ensure no new errors appear
2. **Test with real users** - Have stock managers test the workflow
3. **Check performance** - Monitor notification query performance
4. **Consider enhancements**:
   - WebSocket for real-time notifications
   - Email notifications for critical alerts
   - Notification preferences per user
   - Notification history/archive

---

**Ready to restart?** Just run `mvn -f pom.xml -pl api-module -am spring-boot:run` and test! 🚀
